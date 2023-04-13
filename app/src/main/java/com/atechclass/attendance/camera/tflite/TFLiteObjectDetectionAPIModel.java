package com.atechclass.attendance.camera.tflite;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Trace;
import android.util.Pair;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class TFLiteObjectDetectionAPIModel
        implements SimilarityClassifier {

    private static final int OUTPUT_SIZE = 192;

    // Float model
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;

    // Number of threads in the java app
    private static final int NUM_THREADS = 2;
    private boolean isModelQuantized;
    // Config values.
    private int inputSize;
    // Pre-allocated buffers.
    private Vector<String> labels = new Vector<>();
    private int[] intValues;

    private ByteBuffer imgData;

    private Interpreter tfLite;
    private Interpreter.Options options;

    private Map<String, List<float[]>> registeredFromDb = new HashMap<>();

    public void register(String name, Recognition rec) {
        List<float[]> embList;
        if (registeredFromDb.get(name) != null){
            embList = new ArrayList<>(registeredFromDb.get(name));
        } else {
            embList = new ArrayList<>();
        }
        embList.add(((float[][]) rec.getExtra())[0]);
        registeredFromDb.put(name, embList);
    }

    public TFLiteObjectDetectionAPIModel() {}

    /** Memory-map the model file in Assets. */
    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param assetManager The asset manager to be used to load assets.
     * @param modelFilename The filepath of the model GraphDef protocol buffer.
     * @param labelFilename The filepath of label file for classes.
     * @param inputSize The size of image input
     * @param isQuantized Boolean representing model is quantized or not
     */
    public static SimilarityClassifier create(
            final AssetManager assetManager,
            final String modelFilename,
            final String labelFilename,
            final int inputSize,
            final boolean isQuantized,
            Map<String, List<float[]>> registeredFromDb)
            throws IOException {

        final TFLiteObjectDetectionAPIModel d = new TFLiteObjectDetectionAPIModel();
        d.registeredFromDb = registeredFromDb;
        String actualFilename = labelFilename.split("file:///android_asset/")[1];
        InputStream labelsInput = assetManager.open(actualFilename);
        BufferedReader br = new BufferedReader(new InputStreamReader(labelsInput));
        String line;
        while ((line = br.readLine()) != null) {
            d.labels.add(line);
        }
        br.close();

        d.inputSize = inputSize;

        try {
            d.options = new Interpreter.Options();
            d.options.setNumThreads(NUM_THREADS);
            d.tfLite = new Interpreter(loadModelFile(assetManager, modelFilename), d.options);
        } catch (Exception e) {
            e.printStackTrace();
        }

        d.isModelQuantized = isQuantized;
        // Pre-allocate buffers.
        int numBytesPerChannel;
        if (isQuantized) {
            numBytesPerChannel = 1; // Quantized
        } else {
            numBytesPerChannel = 4; // Floating point
        }
        d.imgData = ByteBuffer.allocateDirect(d.inputSize * d.inputSize * 3 * numBytesPerChannel);
        d.imgData.order(ByteOrder.nativeOrder());
        d.intValues = new int[d.inputSize * d.inputSize];
        return d;
    }

    // looks for the nearest embedding in the dataset
    // and returns the pair <name, distance>
    private Pair<String, Float> findNearest(float[] emb) {
        Pair<String, Float> ret = null;
        if(registeredFromDb.size() > 0) {
            for (Map.Entry<String, List<float[]>> listEntry : registeredFromDb.entrySet()) {
                final String name = listEntry.getKey();
                for(float[] entry : listEntry.getValue()) {
                    if (entry != null) {
                        float distance = 0;
                        for (int i = 0; i < emb.length; i++) {
                            float diff = emb[i] - entry[i];
                            distance += diff * diff;
                        }
                        distance = (float) Math.sqrt(distance);
                        if (ret == null || distance < ret.second) {
                            ret = new Pair<>(name, distance);
                        }
                    }
                }
            }
        }
        return ret;
    }


    @Override
    public List<Recognition> recognizeImage(final Bitmap bitmap, boolean storeExtra) {
        // Log this method so that it can be analyzed with systrace.
        Trace.beginSection("recognizeImage");

        Trace.beginSection("preprocessBitmap");
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        imgData.rewind();
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else { // Float model
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
        }
        Trace.endSection(); // preprocessBitmap

        // Copy the input data into TensorFlow.
        Trace.beginSection("feed");


        Object[] inputArray = {imgData};


        Trace.endSection();

        // Here outputMap is changed to fit the Face Mask detector
        Map<Integer, Object> outputMap = new HashMap<>();

        float[][] embeddings = new float[1][OUTPUT_SIZE];
        outputMap.put(0, embeddings);


        // Run the inference call.
        Trace.beginSection("run");
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);
        Trace.endSection();


        float distance = Float.MAX_VALUE;
        String id = "0";
        String label = "?";

        if (registeredFromDb.size() > 0) {
            final Pair<String, Float> nearest = findNearest(embeddings[0]);
            if (nearest != null) {

                label = nearest.first;
                distance = nearest.second;
            }
        }

        final int numDetectionsOutput = 1;
        final ArrayList<Recognition> recognitions = new ArrayList<>(numDetectionsOutput);
        Recognition rec = new Recognition(
                id,
                label,
                distance,
                new RectF());

        rec.setExtra(embeddings);

        recognitions.add( rec );

        if (storeExtra) {
            rec.setExtra(embeddings);
        }

        Trace.endSection();
        return recognitions;
    }
}