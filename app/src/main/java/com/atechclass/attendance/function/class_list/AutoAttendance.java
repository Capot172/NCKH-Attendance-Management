package com.atechclass.attendance.function.class_list;

import static com.atechclass.attendance.R.string.successful_attendance;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.hardware.camera2.CameraMetadata;
import android.media.AudioManager;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.atechclass.attendance.Language;
import com.atechclass.attendance.R;
import com.atechclass.attendance.adapter.StudentPresentAdapter;
import com.atechclass.attendance.camera.CameraActivity;
import com.atechclass.attendance.camera.customview.OverlayView;
import com.atechclass.attendance.camera.env.BorderedText;
import com.atechclass.attendance.camera.env.ImageUtils;
import com.atechclass.attendance.camera.tflite.Recognition;
import com.atechclass.attendance.camera.tflite.SimilarityClassifier;
import com.atechclass.attendance.camera.tflite.TFLiteObjectDetectionAPIModel;
import com.atechclass.attendance.camera.tracking.MultiBoxTracker;
import com.atechclass.attendance.model.ClassModel;
import com.atechclass.attendance.model.LessonModel;
import com.atechclass.attendance.model.StudentModel;
import com.atechclass.attendance.ultis.UserLogin;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AutoAttendance extends CameraActivity implements OnImageAvailableListener {
    private static final String SUBJECTS_PATH = "Subjects";
    private static final String STUDENTS_PATH = "Students";
    private static final String USER_PATH = "User";
    private static final String LESSON_PATH = "Lesson";

    // MobileFaceNet
    private static final int TF_OD_API_INPUT_SIZE = 112;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_OD_API_MODEL_FILE = "mobile_face_net.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";

    // Minimum detection confidence to track a detection.
    private static final boolean MAINTAIN_ASPECT = false;

    private static final Size DESIRED_PREVIEW_SIZE = new Size(Resources.getSystem().getDisplayMetrics().heightPixels,
        Resources.getSystem().getDisplayMetrics().widthPixels);

    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    private OverlayView trackingOverlay;
    private Integer sensorOrientation;

    private SimilarityClassifier detector;

    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;

    private boolean computingDetection = false;
    private boolean addPending = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker tracker;

    // Face detector
    private FaceDetector faceDetector;

    private Language language;

    // here the preview image is drawn in portrait way
    private Bitmap portraitBmp = null;
    // here the face is cropped and drawn
    private Bitmap faceBmp = null;

    private StudentPresentAdapter studentPresentAdapter;

    private final StorageReference storageReference = FirebaseStorage.getInstance().getReference();

    private boolean checkAttendance = false;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    private LessonModel lessonModel;
    private ClassModel classModel;
    private final List<StudentModel> studentList = new ArrayList<>();
    private final ArrayList<String> students = new ArrayList<>();
    private final Map<String, List<float[]>> registeredFromDb = new HashMap<>();
    
    boolean addFace = false;
    private boolean hasChanged = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        language = new Language(this);
        language.Language();
        mAuth = UserLogin.getAuth();
        db = FirebaseFirestore.getInstance();

        setUpFAB();

        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .build();


        faceDetector = FaceDetection.getClient(options);

    }

    private void setUpFAB() {
        FloatingActionButton fabAdd = findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(view -> onAddClick());

        FloatingActionButton fabStatusAdd = findViewById(R.id.fab_status_add);
        fabStatusAdd.setOnClickListener(view -> {
            if(addFace) {
                fabStatusAdd.setImageResource(R.drawable.ic_add_student);
                bottomListStudent.setVisibility(View.VISIBLE);
                fabAdd.setVisibility(View.GONE);
                addFace = false;
            } else {
                fabStatusAdd.setImageResource(R.drawable.ic_baseline_camera_front);
                addFace = true;
                bottomListStudent.setVisibility(View.GONE);
                fabAdd.setVisibility(View.VISIBLE);
            }
        });

        FloatingActionButton fabBack = findViewById(R.id.fab_back);
        fabBack.setOnClickListener(view -> this.onBackPressed());
    }

    public void studentAttendance(List<StudentModel> studentList) {
        List<StudentModel> newList = new ArrayList<>();
        Map<String, Integer> lessonMap;
        for(StudentModel model : studentList) {
            lessonMap = model.getLessonMap();
            for(Map.Entry<String, Integer> x : lessonMap.entrySet())
                if (x.getValue() == 1 && x.getKey().equals(lessonModel.getId()+"")) {
                    newList.add(model);
                }
        }
        studentPresentAdapter = new StudentPresentAdapter(newList, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rcvStudentsPresent.setLayoutManager(layoutManager);
        rcvStudentsPresent.setAdapter(studentPresentAdapter);
        numPresents.setText(String.valueOf(newList.size()));
        numTotalPresents.setText(String.valueOf(studentList.size()));
    }

    @Override
    protected void onStart() {
        super.onStart();
        user = mAuth.getCurrentUser();
        getData();
        if (user != null) {
            getDataFromDatabase();
        }
    }

    private void getDataFromDatabase() {
        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classModel.getId()+"").collection(STUDENTS_PATH)
                .get().addOnCompleteListener(task -> {
                    studentList.clear();
                    students.clear();
                    for(QueryDocumentSnapshot studentQuery : task.getResult()){
                        StudentModel studentModel = studentQuery.toObject(StudentModel.class);
                        studentList.add(studentModel);
                        List<float[]> embList = new ArrayList<>();
                        String info;
                        if (studentModel.getLastName() != null){
                            info = studentModel.getLastName() + " " + studentModel.getName() + "\t" + studentModel.getId();
                        }
                        else {
                            info = studentModel.getName() + "\t" + studentModel.getId();
                        }
                        students.add(info);
                        if(studentModel.getEmbedding() != null) {
                            for (Map<String, Float> maps : studentModel.getEmbedding()) {
                                float[] embed = new float[maps.size()];
                                for (int i = 0; i < maps.size(); i++)
                                    embed[i] = maps.get(i + "");
                                embList.add(embed);
                            }
                            registeredFromDb.put(info, embList);
                        }
                        if(studentModel.getEmbedding2() != null) {
                            for (Map.Entry<String, Map<String, Float>> entry : studentModel.getEmbedding2().entrySet()){
                                Map<String, Float> maps = entry.getValue();
                                float[] embed = new float[maps.size()];
                                for (int i = 0; i < maps.size(); i++)
                                    embed[i] = maps.get(i + "");
                                embList.add(embed);
                                registeredFromDb.put(info, embList);
                            }
                        }
                    }
                    studentAttendance(studentList);
                });
    }

    private void getData() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        lessonModel = (LessonModel) bundle.getSerializable("lesson");
        classModel = (ClassModel) bundle.getSerializable("class");
    }


    private void onAddClick() {
        addPending = true;
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        BorderedText borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);


        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED,
                            registeredFromDb);
        } catch (final IOException e) {
            e.printStackTrace();
            finish();
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);


        int targetW;
        int targetH;
        if (sensorOrientation == 90 || sensorOrientation == 270) {
            targetH = previewWidth;
            targetW = previewHeight;
        }
        else {
            targetW = previewWidth;
            targetH = previewHeight;
        }
        int cropW = (int) (targetW / 2.0);
        int cropH = (int) (targetH / 2.0);

        croppedBitmap = Bitmap.createBitmap(cropW, cropH, Bitmap.Config.ARGB_8888);
        portraitBmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
        faceBmp = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropW, cropH,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(canvas -> {
            tracker.draw(canvas);
            if (isDebug()) {
                tracker.drawDebug(canvas);
            }
        });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    public String getImageData(Bitmap bitmap) {
        Bitmap bmp = Bitmap.createBitmap(bitmap);
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, bao); // bmp is bitmap from user image file
        bmp.recycle();
        byte[] byteArray = bao.toByteArray();
        return  Base64.encodeToString(byteArray, Base64.URL_SAFE);
        //  store & retrieve this string which is URL safe(can be used to store in FBDB) to firebase
        // Use either Realtime Database or Firestore
    }
    boolean newFace = false;

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;


        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }
        InputImage image = InputImage.fromBitmap(croppedBitmap, 0);
        faceDetector
                .process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.isEmpty()) {
                        updateResults(currTimestamp, new LinkedList<>());
                        return;
                    }
                    runInBackground(() -> {
                        Map<String, Recognition> listFace = onFacesDetected(currTimestamp, faces, addPending, addFace);
                        for (Map.Entry<String, Recognition> entry : listFace.entrySet()) {
                            String face = entry.getKey();
                            Recognition rec = entry.getValue();
                            addPending = false;
                            if (!face.equals("")) {
                                String[] id = face.split("\t");
                                for (StudentModel idStudents : studentList) {
                                    if (idStudents.getId().equals(id[id.length - 1])) {
                                        Map<String, Integer> lessonMap = idStudents.getLessonMap();
                                        if (lessonMap.get(lessonModel.getId() + "") != 1) {
                                            updateStudentDatabase(id, lessonMap, rec);
                                            updateLessonDatabase();
                                            newFace = false;
                                            checkAttendance = true;
                                            beep();
                                            Toast.makeText(this, face + " " + getString(successful_attendance), Toast.LENGTH_SHORT).show();
                                            hasChanged = true;
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    });
                    if(checkAttendance)
                        loadRecyclerData(studentList);
                });
    }

    private void updateStudentDatabase(String[] id, Map<String, Integer> lessonMap, Recognition rec) {
        lessonMap.put(lessonModel.getId() + "", 1);
        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classModel.getId() + "")
                .collection(STUDENTS_PATH).document(id[id.length - 1].trim()).update("lessonMap", lessonMap);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), faceBmp, System.currentTimeMillis() + "", "image");
        Uri uri = Uri.parse(path);

        StorageReference moutainImage = storageReference.child(user.getUid() + "/" + SUBJECTS_PATH + "/" +
                classModel.getId() + "/" + id[id.length - 1].trim() + "/attendance/" + lessonModel.getId() + ".jpg");
        moutainImage.putFile(uri).addOnCompleteListener(task -> {
            File fdelete = new File(getRealPathFromURI(this, uri));
            if (fdelete.exists()) {
                fdelete.delete();
            }
        });

        Map<String, Map<String, Float>> map = new HashMap<>();
        for(StudentModel model : studentList) {
            if(model.getEmbedding2() != null && model.getId().equals(id[id.length - 1].trim())){
                for (Map.Entry<String, Integer> entry : lessonMap.entrySet()){
                    String idLesson = entry.getKey();
                    int value = entry.getValue();
                    if (value != 0){
                        map.put(idLesson, model.getEmbedding2().get(idLesson));
                    }
                }
            }
        }
        map.put(lessonModel.getId() + "", getEmb(rec));

        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classModel.getId() + "").collection(STUDENTS_PATH)
                .document(id[id.length - 1].trim()).update("embedding2", map);
    }

    private void updateLessonDatabase() {
        int absent = 0;
        int present = 0;
        int late = 0;
        for (StudentModel studentModelStatus : studentList){
            int state = studentModelStatus.getLessonMap().getOrDefault(""+ lessonModel.getId(),0);
            switch (state){
                case 0:
                    absent++;
                    break;
                case 1:
                    present++;
                    break;
                default:
                    late++;
            }
        }
        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classModel.getId()+"")
                .collection(LESSON_PATH).document(lessonModel.getId()+"")
                .update("present", present, "absent", absent, "late", late);
    }

    private void beep() {
        ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
        toneG.startTone(ToneGenerator.TONE_PROP_BEEP, 200);
    }

    @Override
    public void onBackPressed() {
        if (hasChanged){
            onReturn();
        }
        finish();
    }

    private void loadRecyclerData(List<StudentModel> studentList) {
        List<StudentModel> newList = new ArrayList<>();
        Map<String, Integer> lessonMap;
        for(StudentModel model : studentList){
            lessonMap = model.getLessonMap();
            for(Map.Entry<String, Integer> x : lessonMap.entrySet())
                if (x.getValue() == 1 && x.getKey().equals(lessonModel.getId()+"")) {
                    newList.add(model);
                }
        }
        studentPresentAdapter.setList(newList);
        rcvStudentsPresent.setAdapter(studentPresentAdapter);
        numPresents.setText(String.valueOf(newList.size()));
    }

    @Override
    protected int getLayoutId() {
        return R.layout.tfe_od_camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    public void onClick(View view) {
        //Do nothing
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        //Do nothing
    }

    // Face Processing
    private Matrix createTransform(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation) {

        Matrix matrix = new Matrix();
        if (applyRotation != 0) {

            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

            // Rotate around origin.
            matrix.postRotate(applyRotation);
        }

//        // Account for the already applied rotation, if any, and then determine how
//        // much scaling is needed for each axis.

        if (applyRotation != 0) {

            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;

    }

    private void showAddFaceDialog(Recognition rec) {
        Dialog dialogLayout = new Dialog(this);
        dialogLayout.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogLayout.setContentView(R.layout.dialog_add_face);
        dialogLayout.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialogLayout.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        ImageView ivFace = dialogLayout.findViewById(R.id.dlg_image);
        TextView tvTitle = dialogLayout.findViewById(R.id.dlg_title);
        Spinner spAddStudent = dialogLayout.findViewById(R.id.spinner);
        Button btnAddFace = dialogLayout.findViewById(R.id.btn_add_face_emb);

        final String[] name = {""};
        final StudentModel[] updateStudent = new StudentModel[1];

        tvTitle.setText(R.string.add_face);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        rec.getCrop().compress(Bitmap.CompressFormat.JPEG, 100, os);

        ivFace.setImageBitmap(rec.getCrop());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, students);
        spAddStudent.setAdapter(adapter);
        spAddStudent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                name[0] = students.get(i);
                updateStudent[0] = studentList.get(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Nothing
            }
        });

        btnAddFace.setOnClickListener(view -> {
            if (name[0].isEmpty()) {
                return;
            }
            detector.register(name[0], rec);
            String path = MediaStore.Images.Media.insertImage(getContentResolver(), rec.getCrop(), System.currentTimeMillis()+"", "image");
            Uri uri = Uri.parse(path);

            StorageReference storageReference = FirebaseStorage.getInstance().getReference();
            StorageReference moutainImage = storageReference.child(user.getUid() + "/" + SUBJECTS_PATH + "/" +
                    classModel.getId() + "/" + updateStudent[0].getId() + "/face_data/" + System.currentTimeMillis() + ".jpg");
            moutainImage.putFile(uri).addOnCompleteListener(task -> {
                File fdelete = new File(getRealPathFromURI(this, uri));
                if (fdelete.exists()) {
                    fdelete.delete();
                }
            });

            List<Map<String, Float>> embed = new ArrayList<>();
            for(StudentModel model : studentList) {
                if(model.getEmbedding() != null && model.getId().equals(updateStudent[0].getId()+""))
                    embed.addAll(model.getEmbedding());
            }
            embed.add(getEmb(rec));

            db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classModel.getId() + "").collection(STUDENTS_PATH)
                    .document(updateStudent[0].getId() + "").update("embedding", embed);


            getDataFromDatabase();

            hasChanged = true;
            dialogLayout.dismiss();
        });

        dialogLayout.show();
    }

    private String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } catch (Exception e) {
            return "";
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private Map<String, Float> getEmb(Recognition rec) {
        Map<String, Float> list = new HashMap<>();
        for (int j = 0; j < ((float[][]) rec.getExtra())[0].length; j ++){
            list.put(j+"", ((float[][]) rec.getExtra())[0][j]);
        }
        return list;
    }

    private void updateResults(long currTimestamp, final List<Recognition> mappedRecognitions) {

        tracker.trackResults(mappedRecognitions, currTimestamp);
        trackingOverlay.postInvalidate();
        computingDetection = false;

        if(addFace && mappedRecognitions.size() > 0){
            Recognition rec = mappedRecognitions.get(0);
            if (rec.getExtra() != null) {
                if (rec.getCrop() != null){
                    showAddFaceDialog(rec);
                } else {
                    Toast.makeText(this, getString(R.string.tutorial_attendance), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private Map<String, Recognition> onFacesDetected(long currTimestamp, List<Face> faces, boolean add, boolean addFace) {
        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);

        final List<Recognition> mappedRecognitions = new LinkedList<>();

        // Note this can be done only once
        int sourceW = rgbFrameBitmap.getWidth();
        int sourceH = rgbFrameBitmap.getHeight();
        int targetW = portraitBmp.getWidth();
        int targetH = portraitBmp.getHeight();
        Matrix transform = createTransform(
                sourceW,
                sourceH,
                targetW,
                targetH,
                sensorOrientation);
        final Canvas cv = new Canvas(portraitBmp);

        // draws the original image in portrait mode.
        cv.drawBitmap(rgbFrameBitmap, transform, null);

        final Canvas cvFace = new Canvas(faceBmp);

        Map<String, Recognition> name = new HashMap<>();

        if(addFace)
            addAFace(faces, add, mappedRecognitions, transform, cvFace, name);
        else
            multiFaces(faces, add, mappedRecognitions, transform, cvFace, name);

        updateResults(currTimestamp, mappedRecognitions);

        return name;
    }

    private void addAFace(List<Face> faces, boolean add, List<Recognition> mappedRecognitions, Matrix transform, Canvas cvFace, Map<String, Recognition> name) {
        String label = "";
        float confidence = -1f;
        int color = getResources().getColor(R.color.btn_color_primary);
        Object extra = null;
        Bitmap crop = null;

        Face face = faces.get(0);
        final RectF boundingBox = new RectF(face.getBoundingBox());

        // maps crop coordinates to original
        cropToFrameTransform.mapRect(boundingBox);

        // maps original coordinates to portrait coordinates
        RectF faceBB = new RectF(boundingBox);
        transform.mapRect(faceBB);

        // translates portrait to origin and scales to fit input inference size
        float sx = (TF_OD_API_INPUT_SIZE) / faceBB.width();
        float sy = (TF_OD_API_INPUT_SIZE) / faceBB.height();
        Matrix matrix = new Matrix();
        matrix.postTranslate(-faceBB.left, -faceBB.top);
        matrix.postScale(sx, sy);

        cvFace.drawBitmap(portraitBmp, matrix, null);

        if (add && faceBB.left >= 0 && faceBB.left + faceBB.width() <= portraitBmp.getWidth()) {
            crop = Bitmap.createBitmap(portraitBmp,
                    (int) faceBB.left,
                    (int) faceBB.top,
                    (int) faceBB.width(),
                    (int) faceBB.height());
        }

        //Create list to recognize faces
        final List<Recognition> resultsAux = detector.recognizeImage(faceBmp, add);

        if (resultsAux.size() > 0) {

            Recognition result = resultsAux.get(0);

            extra = result.getExtra();

        }

        if (getCameraFacing() == CameraMetadata.LENS_FACING_FRONT) {

            // camera is frontal so the image is flipped horizontally
            // flips horizontally
            Matrix flip = new Matrix();
            if (sensorOrientation == 90 || sensorOrientation == 270) {
                flip.postScale(1, -1, previewWidth / 2.0f, previewHeight / 2.0f);
            }
            else {
                flip.postScale(-1, 1, previewWidth / 2.0f, previewHeight / 2.0f);
            }
            flip.mapRect(boundingBox);

        }

        final Recognition result = new Recognition(
                "0", label, confidence, boundingBox);

        result.setColor(color);
        result.setLocation(boundingBox);
        result.setExtra(extra);
        result.setCrop(crop);
        mappedRecognitions.add(result);

        name.put(label, result);
    }

    private void multiFaces(List<Face> faces, boolean add, List<Recognition> mappedRecognitions, Matrix transform, Canvas cvFace, Map<String, Recognition> name) {
        for (Face face: faces){
            String label = "";
            float confidence = -1f;
            int color = getResources().getColor(R.color.pastel_red );
            Object extra = null;
            Bitmap crop = null;
            final RectF boundingBox = new RectF(face.getBoundingBox());

            // maps crop coordinates to original
            cropToFrameTransform.mapRect(boundingBox);

            // maps original coordinates to portrait coordinates
            RectF faceBB = new RectF(boundingBox);
            transform.mapRect(faceBB);

            // translates portrait to origin and scales to fit input inference size
            float sx = (TF_OD_API_INPUT_SIZE) / faceBB.width();
            float sy = (TF_OD_API_INPUT_SIZE) / faceBB.height();
            Matrix matrix = new Matrix();
            matrix.postTranslate(-faceBB.left, -faceBB.top);
            matrix.postScale(sx, sy);

            cvFace.drawBitmap(portraitBmp, matrix, null);

            if (add && faceBB.left >= 0 && faceBB.left + faceBB.width() <= portraitBmp.getWidth()) {
                crop = Bitmap.createBitmap(portraitBmp,
                        (int) faceBB.left,
                        (int) faceBB.top,
                        (int) faceBB.width(),
                        (int) faceBB.height());
            }

            //Create list to recognize faces
            final List<Recognition> resultsAux = detector.recognizeImage(faceBmp, add);

            if (resultsAux.size() > 0) {

                Recognition result = resultsAux.get(0);

                extra = result.getExtra();

                float conf = result.getDistance();
                if (conf < 0.8f) {
                    confidence = conf;
                    label = result.getTitle();
                    if (result.getId().equals("0")) {
                        color = getResources().getColor(R.color.pastel_green);
                    }
                    else {
                        color = Color.RED;
                    }
                }

            }

            if (getCameraFacing() == CameraMetadata.LENS_FACING_FRONT) {

                // camera is frontal so the image is flipped horizontally
                // flips horizontally
                Matrix flip = new Matrix();
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    flip.postScale(1, -1, previewWidth / 2.0f, previewHeight / 2.0f);
                }
                else {
                    flip.postScale(-1, 1, previewWidth / 2.0f, previewHeight / 2.0f);
                }
                flip.mapRect(boundingBox);

            }

            final Recognition result = new Recognition(
                    "0", label, confidence, boundingBox);

            result.setColor(color);
            result.setLocation(boundingBox);
            result.setExtra(extra);
            result.setCrop(crop);
            mappedRecognitions.add(result);

            name.put(label, result);
        }
    }

}