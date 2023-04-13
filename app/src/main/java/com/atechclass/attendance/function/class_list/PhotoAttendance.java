package com.atechclass.attendance.function.class_list;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.atechclass.attendance.R;
import com.atechclass.attendance.adapter.ListFaceStudentsAdapter;
import com.atechclass.attendance.databinding.ActivityPhotoAttendanceBinding;
import com.atechclass.attendance.interfaces.IOnClickAddStudents;
import com.atechclass.attendance.model.ClassModel;
import com.atechclass.attendance.model.FaceStudent;
import com.atechclass.attendance.model.LessonModel;
import com.atechclass.attendance.model.StudentModel;
import com.atechclass.attendance.ultis.UserLogin;
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

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhotoAttendance extends AppCompatActivity implements IOnClickAddStudents {
    private ActivityPhotoAttendanceBinding binding;
    private Uri imageUri;
    private ListFaceStudentsAdapter adapterFaceStudents;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private LessonModel lessonModel;
    private ClassModel classModel;
    private Intent intent;
    private FaceDetector detector;

    private float[][] embeedings;
    private int[] intValues;
    private final float IMAGE_MEAN = 128.0f;
    private final float IMAGE_STD = 128.0f;
    private final int OUTPUT_SIZE=192; //Output size of model
    private final int INPUT_SIZE = 112;
    private Interpreter tfLite;

    private String name = "?";
    private String id = "0";
    private boolean presented = false;
    private boolean modeAttendance = true;
    private Pair<String, float[][]> emb;

    private final List<StudentModel> studentList = new ArrayList<>();
    private List<FaceStudent> faceStudents = new ArrayList<>();
    private final ArrayList<Float> knownEmbeddings = new ArrayList<>();
    private final ArrayList<String> students = new ArrayList<>();
    private final Map<String, List<float[]>> registeredFromDb = new HashMap<>();

    private final StorageReference storageReference = FirebaseStorage.getInstance().getReference();

    private static final String SUBJECTS_PATH = "Subjects";
    private static final String STUDENTS_PATH = "Students";
    private static final String USER_PATH = "User";
    private static final String LESSON_PATH = "Lesson";
    private final String modelFile="mobile_face_net.tflite"; //model name

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_attendance);
        mAuth = UserLogin.getAuth();
        db = FirebaseFirestore.getInstance();
        getData();
        //Set for tfLite
        try {
            tfLite=new Interpreter(loadModelFile(PhotoAttendance.this, modelFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_photo_attendance);
        openCamera();
        onChangeMode();
        if(faceStudents.size() == 0)
            changeLayout(false);
    }

    private void onChangeMode() {
        binding.btnAtendancePhoto.setOnClickListener(view -> {
            Intent gallery = new Intent(Intent.ACTION_PICK);
            gallery.setType("image/*");
            startActivityForResult(gallery, 104);
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        super.onBackPressed();
    }

    private MappedByteBuffer loadModelFile(Activity activity, String MODEL_FILE) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    //Lấy dữ liệu từ class attendance
    private void getData() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        lessonModel = (LessonModel) bundle.getSerializable("lesson");
        classModel = (ClassModel) bundle.getSerializable("class");
    }

    @Override
    protected void onStart() {
        super.onStart();
        user = mAuth.getCurrentUser();
        if (user != null) {
            getDataFromDatabase();
        }
    }

    private void changeLayout(boolean change) {
        if(change) {
            binding.gridListFaces.setVisibility(View.VISIBLE);
            binding.layoutEmpty.setVisibility(View.GONE);
        } else {
            binding.gridListFaces.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.VISIBLE);
        }
    }

    private void getDataFromDatabase() {
        //Lấy dữ liệu từ firebase
        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH)
                .document(classModel.getId()+"").collection(STUDENTS_PATH)
                .get().addOnCompleteListener(task -> {
                    studentList.clear();
                    knownEmbeddings.clear();
                    students.clear();
                    for(QueryDocumentSnapshot studentQuery : task.getResult()){
                        StudentModel studentModel = studentQuery.toObject(StudentModel.class);
                        studentList.add(studentModel);
                        String info;
                        if (studentModel.getLastName() != null){
                            info = studentModel.getLastName() + " " + studentModel.getName() + "\t" + studentModel.getId();
                        }
                        else {
                            info = studentModel.getName() + "\t" + studentModel.getId();
                        }
                        students.add(info);
                        if(studentModel.getEmbedding() != null) {
                            List<float[]> embList = new ArrayList<>();
                            for (Map<String, Float> maps : studentModel.getEmbedding()) {
                                float[] embed = new float[maps.size()];
                                for (int i = 0; i < maps.size(); i++)
                                    embed[i] = maps.get(i + "");
                                embList.add(embed);
                            }
                            registeredFromDb.put(info, embList);
                        }
                        if(studentModel.getEmbedding2() != null) {
                            List<float[]> embList = new ArrayList<>();
                            for (Map.Entry<String, Map<String, Float>> entry : studentModel.getEmbedding2().entrySet()) {
                                Map<String, Float> maps = entry.getValue();
                                float[] embed = new float[maps.size()];
                                for (int i = 0; i < maps.size(); i++)
                                    embed[i] = maps.get(i + "");
                                embList.add(embed);
                            }
                            registeredFromDb.put(info, embList);
                        }
                    }
                    totalPresent();
                });
    }

    private void totalPresent() {
        List<StudentModel> listPresent = new ArrayList<>();
        Map<String, Integer> lessonMap;
        for(StudentModel model : studentList){
            lessonMap = model.getLessonMap();
            for(Map.Entry<String, Integer> x : lessonMap.entrySet())
                if (x.getValue() == 1 && x.getKey().equals(lessonModel.getId()+"")) {
                    listPresent.add(model);
                }
        }
        binding.presentPicture.setText(listPresent.size() + "");
        binding.sumPresentPicture.setText(studentList.size() + "");
    }

    private void openCamera() {
        binding.btnAtendanceCamera.setOnClickListener(view -> {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, 103);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap thumbnail = null;
        if(requestCode == 103 && resultCode == -1 && imageUri != null) {
            binding.loadingPanel.setVisibility(View.VISIBLE);
            try {
                thumbnail = MediaStore.Images.Media.getBitmap(
                        getContentResolver(), imageUri);
                thumbnail = getImageRotation(thumbnail, imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            handleImage(thumbnail);
        }
        if(requestCode == 104 && data != null && resultCode == RESULT_OK) {
            binding.loadingPanel.setVisibility(View.VISIBLE);
            Uri uri = data.getData();
            try {
                thumbnail = MediaStore.Images.Media.getBitmap(
                        getContentResolver(), uri);
                thumbnail = getImageRotation(thumbnail, uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            handleImage(thumbnail);
        }
    }

    private void handleImage(Bitmap thumbnail) {
        if(thumbnail != null) {
            FaceDetectorOptions options =
                    new FaceDetectorOptions.Builder()
                            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                            .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                            .build();

            detector = FaceDetection.getClient(options);
            InputImage image = InputImage.fromBitmap(thumbnail, 0);
            recognizeFace(image, thumbnail);
        }
    }

    private void recognizeFace(InputImage image, Bitmap thumbnail) {
        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if(faces.size() > 0) {
                        for (Face face : faces) {
                            id = "";
                            name = "";
                            final RectF boundingBox = new RectF(face.getBoundingBox());
                            if (boundingBox != null) {
                                //Lấy dữ liệu hộp của gương mặt
                                Bitmap cropped_face = getCropBitmapByCPU(thumbnail, boundingBox);
                                //resize lại theo định dạng 112*112
                                Bitmap scaled = getResizedBitmap(cropped_face, INPUT_SIZE, INPUT_SIZE);
                                //nếu tìm được đối tượng
                                Pair<String, float[][]> student = getStudent(scaled);
                                if(!student.first.equals("")) {
                                    String[] infoStudent = student.first.split("\t");
                                    //Lấy id
                                    id = infoStudent[infoStudent.length - 1];
                                    //Lấy name
                                    name = infoStudent[infoStudent.length - 2];
                                    //Kiểm tra danh sách trùng
                                    if(faceStudents.size() > 0) {
                                        for(FaceStudent faceStudent : faceStudents) {
                                            if(faceStudent.getiDFace().equals(id))
                                                presented = true;
                                        }
                                    }
                                    if(!presented)
                                        faceStudents.add(new FaceStudent(scaled, id, name, student.second));
                                    else presented = false;
                                } else {
                                    faceStudents.add(new FaceStudent(scaled, "", "", student.second));
                                }
                            }
                        }
//                        getDataFromDatabase();
                        adapterFaceStudents = new ListFaceStudentsAdapter(this, faceStudents, this);
                        binding.gridListFaces.setAdapter(adapterFaceStudents);
                        attendance(faceStudents);
                        binding.loadingPanel.setVisibility(View.GONE);
                        changeLayout(faceStudents.size() > 0);
                    }
                });
    }

    private Bitmap getImageRotation(Bitmap img, Uri uri) throws IOException {
        InputStream input = getContentResolver().openInputStream(uri);
        //Đọc thẻ định dạng jpg
        ExifInterface ei;
        if (Build.VERSION.SDK_INT > 23)
            ei = new ExifInterface(input);
        else
            ei = new ExifInterface(uri.getPath());

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }
    //Xoay ảnh
    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    //Điểm danh
    private void attendance(List<FaceStudent> faceStudents) {
        for(StudentModel studentModel : studentList) {
            for(FaceStudent faceStudent : faceStudents) {
                if (studentModel.getId().equals(faceStudent.getiDFace())) {
                    Map<String, Integer> lessonMap = studentModel.getLessonMap();
                    if (lessonMap.get(lessonModel.getId() + "") != 1) {
                        lessonMap.put(lessonModel.getId() + "", 1);
                        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classModel.getId() + "")
                                .collection(STUDENTS_PATH).document(faceStudent.getiDFace()).update("lessonMap", lessonMap);

                        String path = MediaStore.Images.Media.insertImage(getContentResolver(), faceStudent.getImgFace(), System.currentTimeMillis() + "", "image");
                        Uri uri = Uri.parse(path);

                        StorageReference moutainImage = storageReference.child(user.getUid() + "/" + SUBJECTS_PATH + "/" +
                                classModel.getId() + "/" + faceStudent.getiDFace() + "/attendance/" + lessonModel.getId() + ".jpg");
                        moutainImage.putFile(uri).addOnCompleteListener(task -> {
                            File fdelete = new File(getRealPathFromURI(this, uri));
                            if (fdelete.exists()) {
                                fdelete.delete();
                            }
                        });
//                        List<Map<String, Float>> embed2 = new ArrayList<>();
//                        if(studentModel.getEmbedding2() != null){
//                            int i = 0;
//                            for (Map.Entry<String, Integer> entry : lessonMap.entrySet()){
//                                int value = entry.getValue();
//                                if (value != 0){
//                                    embed2.add(studentModel.getEmbedding2().get(i));
//                                }
//                                i++;
//                            }
//                        }
//
//                        embed2.add(getEmb(faceStudent.getEmbs()));

                        Map<String, Map<String, Float>> map = new HashMap<>();
                        for(StudentModel model : studentList) {
                            if(model.getEmbedding2() != null){
                                for (Map.Entry<String, Integer> entry : lessonMap.entrySet()){
                                    String idLesson = entry.getKey();
                                    int value = entry.getValue();
                                    if (value != 0){
                                        map.put(idLesson, model.getEmbedding2().get(idLesson));
                                    }
                                }
                            }
                        }
                        map.put(lessonModel.getId() + "", getEmb(faceStudent.getEmbs()));

                        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classModel.getId() + "").collection(STUDENTS_PATH)
                                .document(faceStudent.getiDFace()).update("embedding2", map);
                        updateLessonDatabase();
                    }
                    break;
                }
            }
        }
    }

    private Pair<String, float[][]> getStudent(Bitmap bitmap) {
        ByteBuffer imgData = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4);
        imgData.order(ByteOrder.nativeOrder());
        intValues = new int[INPUT_SIZE * INPUT_SIZE];
        //get pixel values from Bitmap to normalize
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        imgData.rewind();

        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                int pixelValue = intValues[i * INPUT_SIZE + j];
                imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
            }
        }

        Object[] inputArray = {imgData};
        // Here outputMap is changed to fit the Face Mask detector
        Map<Integer, Object> outputMap = new HashMap<>();

        embeedings = new float[1][OUTPUT_SIZE];
        outputMap.put(0, embeedings);

        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);

        float distance;
        String name;
        if (registeredFromDb.size() > 0) {
            final Pair<String, Float> nearest = findNearest(embeedings[0]);
            if (nearest != null) {
                name = nearest.first;
                distance = nearest.second;
                if (distance <= .8f) {
                    emb = new Pair<>(name, embeedings);
                    return emb;
                }
                emb = new Pair<>("", embeedings);
            }
        } else {

            emb = new Pair<>("", embeedings);
        }
        return emb;
    }

    private Map<String, Float> getEmb(float[][] embeddings) {
        Map<String, Float> map = new HashMap<>();
        for(int j = 0; j < embeddings[0].length; j++)
            map.put(j+"", embeddings[0][j]);
        return map;
    }

    // looks for the nearest embeeding in the dataset (using L2 norm)
    // and retrurns the pair <id, distance>
    private Pair<String, Float> findNearest(float[] emb) {
        Pair<String, Float> ret = null;
        if(registeredFromDb.size() > 0) {
            for (Map.Entry<String, List<float[]>> listEntry : registeredFromDb.entrySet()) {
                if(listEntry != null) {
                    final String name = listEntry.getKey();
                    for(float[] entry : listEntry.getValue()) {
                        final float[] knownEmb = entry;
                        float distance = 0;
                        for (int i = 0; i < emb.length; i++) {
                            float diff = 0f;
                            if(emb.length > 0)
                                diff = emb[i] - knownEmb[i];
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

    private Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    private Bitmap getCropBitmapByCPU(Bitmap source, RectF cropRectF) {
        Bitmap resultBitmap = Bitmap.createBitmap((int) cropRectF.width(),
                (int) cropRectF.height(), Bitmap.Config.ARGB_8888);
        Canvas cavas = new Canvas(resultBitmap);

        // draw background
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setColor(Color.WHITE);
        cavas.drawRect(
                new RectF(0, 0, cropRectF.width(), cropRectF.height()),
                paint);

        Matrix matrix = new Matrix();
        matrix.postTranslate(-cropRectF.left, -cropRectF.top);

        cavas.drawBitmap(source, matrix, paint);

        return resultBitmap;
    }

    public void onBack(View view) {
        this.onBackPressed();
    }

    @Override
    public void addStudents(int i, Bitmap faceCrop, float[][] embs) {
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
        final String[] oldAvatar = {""};

        tvTitle.setText("Add Face");
        ivFace.setImageBitmap(faceCrop);
        String avatar = getImageData(faceCrop);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, students);
        spAddStudent.setAdapter(adapter);
        spAddStudent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                name[0] = students.get(i);
                updateStudent[0] = studentList.get(i);
//                oldAvatar[0] = studentList.get(i).getAvatar();
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

            List<Map<String, Float>> embed = new ArrayList<>();
            for(StudentModel model : studentList) {
                if(model.getEmbedding() != null && model.getId().equals(updateStudent[0].getId()+""))
                    for(Map<String, Float> map : model.getEmbedding())
                        embed.add(map);
            }

            embed.add(getEmb(embs));
            String[] infoStudent = name[0].split("\t");
            String idStudent = id = infoStudent[infoStudent.length - 1];
            String nameStudent = infoStudent[infoStudent.length - 2];
            faceStudents.get(i).setiDFace(idStudent);
            faceStudents.get(i).setNameFace(nameStudent);
            adapterFaceStudents.updateList(faceStudents);
            for (StudentModel idStudents : studentList) {
                if (idStudents.getId().equals(id)) {
                    Map<String, Integer> lessonMap = idStudents.getLessonMap();
                    if (lessonMap.get(lessonModel.getId() + "") != 1) {
                        lessonMap.put(lessonModel.getId() + "", 1);
                        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classModel.getId() + "")
                                .collection(STUDENTS_PATH).document(id.trim()).update("lessonMap", lessonMap);

                        String path = MediaStore.Images.Media.insertImage(getContentResolver(), faceStudents.get(i).getImgFace(), System.currentTimeMillis() + "", "image");
                        Uri uri = Uri.parse(path);

                        StorageReference moutainImage = storageReference.child(user.getUid() + "/" + SUBJECTS_PATH + "/" +
                                classModel.getId() + "/" + faceStudents.get(i).getiDFace() + "/attendance/" + lessonModel.getId() + ".jpg");
                        moutainImage.putFile(uri).addOnCompleteListener(task -> {
                            File fdelete = new File(getRealPathFromURI(this, uri));
                            if (fdelete.exists()) {
                                fdelete.delete();
                            }
                        });
                    }
                    break;
                }
            }

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            faceCrop.compress(Bitmap.CompressFormat.JPEG, 100, os);
            String path = MediaStore.Images.Media.insertImage(getContentResolver(), faceCrop, System.currentTimeMillis()+"", "image");
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

//            if(oldAvatar[0] == null) {
//                db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classModel.getId() + "").collection(STUDENTS_PATH)
//                        .document(updateStudent[0].getId() + "").update( "avatar", avatar);
//            }
            db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classModel.getId() + "").collection(STUDENTS_PATH)
                    .document(updateStudent[0].getId() + "").update("embedding", embed);

            getDataFromDatabase();

            setResult(RESULT_OK);

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
                .update("present", present, "absent", absent, "late", late).addOnCompleteListener(task -> getDataFromDatabase());
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
}