package com.atechclass.attendance.function.class_list.Student;

import static com.atechclass.attendance.ultis.HandleAvatar.getCropBitmapByCPU;
import static com.atechclass.attendance.ultis.HandleAvatar.getCropBitmapByCPUAvatar;
import static com.atechclass.attendance.ultis.HandleAvatar.getEmb;
import static com.atechclass.attendance.ultis.HandleAvatar.getImageRotation;
import static com.atechclass.attendance.ultis.HandleAvatar.getResizedBitmap;
import static com.atechclass.attendance.ultis.HandleAvatar.getResizedBitmapAvatar;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atechclass.attendance.Language;
//import com.atechclass.attendance.R;
import com.atechclass.attendance.adapter.FaceEmbAdapter;
import com.atechclass.attendance.adapter.StudentSubClassListAdapter;
import com.atechclass.attendance.function.class_list.Attendance;
import com.atechclass.attendance.interfaces.IOnClickLessonStudentInfo;
import com.atechclass.attendance.interfaces.OnClickItem;
import com.atechclass.attendance.model.ClassModel;
import com.atechclass.attendance.model.LessonModel;
import com.atechclass.attendance.model.StudentSubClassList;
import com.atechclass.attendance.model.StudentModel;
import com.atechclass.attendance.ultis.PieCharAnalysis;
import com.atechclass.attendance.ultis.UserLogin;
import com.atechclass.attendance.R;
import com.github.mikephil.charting.charts.PieChart;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.hdodenhof.circleimageview.CircleImageView;

public class StudentInfo extends AppCompatActivity implements IOnClickLessonStudentInfo, OnClickItem {

    private static final String SUBJECTS_PATH = "Subjects";
    private static final String STUDENTS_PATH = "Students";
    private static final String USER_PATH = "User";
    private static final String LESSON_PATH = "Lesson";

    private static final String MOBILE_FACE_NET_TFLITE ="mobile_face_net.tflite"; //model name

    private static final int INPUT_SIZE = 112;
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;
    private static final int OUTPUT_SIZE=192; //Output size of model

    private Language language;

    private final List<StudentSubClassList> infoClasses = new ArrayList<>();
    private final List<LessonModel> lessonModels = new ArrayList<>();
    private final ArrayList<String> lessonIdList = new ArrayList<>();
    private List<String> studentId = new ArrayList<>();
    private Map<String, Integer> lessonMap = new HashMap<>();
    private StudentModel studentModelRoot;
    private ClassModel classRoot;
    private String studentRootID;
    private int present = 0;
    private int absent = 0;
    private int late = 0;

    private PieChart chartStudentSub;

    private RecyclerView recyclerView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    private TextView tvStudentName;
    private TextView tvStudentID;
    private TextView tvStudentPhone;
    private TextView tvStudentEmail;
    private ImageButton btnBack;
    private ImageButton btnEdit;
    private ImageButton btnAddDataFace;
    private CircleImageView circleImageView;
    private CircleImageView imgAvatarData;
    private Button btnCancelData;
    private Button btnAddData;

    private TableRow emailRow;
    private TableRow phoneRow;

    private StorageReference storageReference = FirebaseStorage.getInstance().getReference();
    private StorageReference mountains;
    private StorageReference faceDataStorage;

    private List<StorageReference> uri = new ArrayList<>();
    private FaceEmbAdapter adapter;
    private RecyclerView view;

    private CircleImageView avatar;
    private Uri imageUri;
    private Bitmap thumbnail;
    private Bitmap thumbnailFace;
    private Bitmap avatarStudentInfo;
    private Bitmap avatarStudent = null;
    private ImageView imgAvatar;
    private FaceDetector detector;

    private Interpreter tfLite;
    private List<Map<String, Float>> embed = new ArrayList<>();
    private Map<String, Float> embedTemporary = new HashMap<>();

    /* Data Flow:
        1) Get data from bundle, which include: a root student model, a root class, a list of student's ids
        2) Get attendance values from lessonMap in the student model
        3) Get lesson list from database
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        language = new Language(this);
        language.Language();
        setContentView(R.layout.activity_student_infor);

        ORM();

        mAuth = UserLogin.getAuth();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();
        if (user != null) {
            Intent intent = getIntent();
            Bundle bundle = intent.getExtras();
            studentRootID =  bundle.getString("student");
            classRoot = (ClassModel) bundle.getSerializable("class");
            studentId = bundle.getStringArrayList("studentId");

            view = findViewById(R.id.list_data_face);
            getStudentData();
        }
        //Set for tfLite
        try {
            tfLite=new Interpreter(loadModelFile(this));
        } catch (IOException e) {
            e.printStackTrace();
        }

        setButtons();
        seeAvatar();
        addDataFace();
    }

    private void addDataFace() {
        btnAddDataFace.setOnClickListener(view1 -> {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                if(Boolean.TRUE.equals(getFromPerf(this, "ALLOW_KEY"))) {
                    //Access setting
                    showSetting();
                } else if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
                }
            } else {
                Dialog dialog = new Dialog(this);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.dialog_add_data_face);
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                imgAvatarData = dialog.findViewById(R.id.img_avatar_data);
                btnCancelData = dialog.findViewById(R.id.btn_cancel_data);
                btnAddData = dialog.findViewById(R.id.btn_add_data);

                btnCancelData.setOnClickListener(view2 -> {
                    dialog.dismiss();
                });

                imgAvatarData.setOnClickListener(view2 -> {
                    openDialogChangeAvatar(103, 104);
                });

                btnAddData.setOnClickListener(view2 -> {
                    if(thumbnailFace != null) {
                        embed.add(embedTemporary);

                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        thumbnailFace.compress(Bitmap.CompressFormat.JPEG, 100, os);
                        String path = MediaStore.Images.Media.insertImage(getContentResolver(), thumbnailFace, System.currentTimeMillis()+"", "image");
                        Uri uriFace = Uri.parse(path);

                        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
                        StorageReference moutainImage = storageReference.child(user.getUid() + "/" + SUBJECTS_PATH + "/" +
                                classRoot.getId() + "/" + studentModelRoot.getId() + "/face_data/" + System.currentTimeMillis() + ".jpg");
                        moutainImage.putFile(uriFace).addOnCompleteListener(task -> {
                            File fdelete = new File(getRealPathFromURI(this, uriFace));
                            if (fdelete.exists())
                                fdelete.delete();
                            getStudentData();
                        });

                        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId() + "").collection(STUDENTS_PATH)
                                .document(studentModelRoot.getId() + "").update("embedding", embed).addOnSuccessListener(task -> {
                                    thumbnailFace = null;
                                    thumbnail = null;
                                    dialog.dismiss();
                                });
                    } else {
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }
        });
    }

    private void getListEmbFace() {
        mountains = storageReference.child(user.getUid() + "/" + SUBJECTS_PATH + "/" +
                classRoot.getId() + "/" + studentRootID + "/face_data");
        mountains.listAll().addOnSuccessListener(listResult -> {
            uri.clear();
            uri.addAll(listResult.getItems());
            adapter = new FaceEmbAdapter(uri, this);
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setOrientation(RecyclerView.HORIZONTAL);
            view.setLayoutManager(layoutManager);
            view.setAdapter(adapter);
        });
    }

    private void seeAvatar() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_show_image);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        imgAvatar = dialog.findViewById(R.id.img_see_avatar);
        ConstraintLayout parentDialog = dialog.findViewById(R.id.paren_show_avatar);
        this.avatar.setOnClickListener(view -> {
            parentDialog.setOnClickListener(view2 -> dialog.dismiss());
            dialog.show();
        });
    }

    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(StudentInfo.MOBILE_FACE_NET_TFLITE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 103 && resultCode == RESULT_OK && imageUri != null) {
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
            imageUri = data.getData();
            try {
                thumbnail = MediaStore.Images.Media.getBitmap(
                        getContentResolver(), imageUri);
                thumbnail = getImageRotation(thumbnail, imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            handleImage(thumbnail);
        }
        if (requestCode == 105 && resultCode == RESULT_OK){
            getStudentData();
            setResult(RESULT_OK);
        }
        if(requestCode == 106 && resultCode == RESULT_OK && imageUri != null) {
            try {
                avatarStudent = MediaStore.Images.Media.getBitmap(
                        getContentResolver(), imageUri);
                avatarStudent = getImageRotation(avatarStudent, imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(imgAvatar != null)
                imgAvatar.setImageBitmap(avatarStudent);
            if(circleImageView != null)
                circleImageView.setImageBitmap(avatarStudent);
        }
        if(requestCode == 107 && data != null && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            try {
                avatarStudent = MediaStore.Images.Media.getBitmap(
                        getContentResolver(), uri);
                avatarStudent = getImageRotation(avatarStudent, uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(imgAvatar != null)
                imgAvatar.setImageBitmap(avatarStudent);
            if(circleImageView != null)
                circleImageView.setImageBitmap(avatarStudent);
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
            recognizeFace(image);
        }
    }

    private void recognizeFace(InputImage image) {
        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if(!faces.isEmpty()) {
                        final RectF boundingBoxAvatar = new RectF(faces.get(0).getBoundingBox());
                        final RectF boundingBoxFace = new RectF(faces.get(0).getBoundingBox());
                        //Lấy dữ liệu hộp của gương mặt
                        Bitmap croppedFaceAvatar = getCropBitmapByCPUAvatar(thumbnail, boundingBoxAvatar);
                        //resize lại theo định dạng 112*112
                        Bitmap scaledAvatar = getResizedBitmap(croppedFaceAvatar, 256, 256);
                        //Lấy dữ liệu hộp của gương mặt
                        Bitmap croppedFace = getCropBitmapByCPU(thumbnail, boundingBoxFace);
                        //resize lại theo định dạng 112*112
                        Bitmap scaledFace = getResizedBitmap(croppedFace, 112, 112);
                        if(circleImageView != null)
                            circleImageView.setImageBitmap(scaledAvatar);
                        if(imgAvatarData != null) {
                            imgAvatarData.setImageBitmap(scaledAvatar);
                        }
                        thumbnailFace = scaledAvatar;
                        thumbnail = scaledFace;
                        getEmbFace(scaledFace);
                    }
                });
    }

    private void getEmbFace(Bitmap bitmap) {
        ByteBuffer imgData = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3 * 4);
        imgData.order(ByteOrder.nativeOrder());
        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
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

        float[][] embeddings = new float[1][OUTPUT_SIZE];
        outputMap.put(0, embeddings);
        embedTemporary.clear();
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);
        embedTemporary = getEmb(embeddings);
    }

    private void getLessonsData() {
        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId()+"").collection(LESSON_PATH)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        lessonModels.clear();
                        lessonIdList.clear();
                        for (QueryDocumentSnapshot lessonSnapshot : task.getResult()) {
                            LessonModel lessonModel = lessonSnapshot.toObject(LessonModel.class);
                            lessonModels.add(lessonModel);
                            lessonIdList.add(lessonModel.getId()+"");
                        }
                        CompletableFuture<Boolean> lessonFuture = initList();
                        lessonFuture.thenAccept(isSuccess -> updateListAttendance());
                        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                    }
                });

    }

    private void getStudentData() {
        TextView tvTotal = findViewById(R.id.tv_total);
        TextView tvPercent = findViewById(R.id.tv_percent);
        TextView tvPresent = findViewById(R.id.tv_present);
        TextView tvAbsent = findViewById(R.id.tv_absent);
        TextView tvLate = findViewById(R.id.tv_late);
        //Get data from bundle
        findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId()+"").collection(STUDENTS_PATH)
                .document(studentRootID).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        studentModelRoot = task.getResult().toObject(StudentModel.class);
                        assert studentModelRoot != null;
                        if(studentModelRoot.getEmbedding() != null){
                            embed = new ArrayList<>(studentModelRoot.getEmbedding());
                        } else {
                            embed = new ArrayList<>();
                        }

                        lessonMap = studentModelRoot.getLessonMap();
                        getLessonsData();
                        //Set text view
                        String fullName;
                        if (studentModelRoot.getLastName() != null){
                            fullName = studentModelRoot.getLastName() + " " + studentModelRoot.getName();
                        } else {
                            fullName = studentModelRoot.getName();
                        }
                        tvStudentName.setText(fullName);
                        tvStudentID.setText(studentModelRoot.getId());
                        tvStudentPhone.setText(studentModelRoot.getPhoneNumber());
                        tvStudentEmail.setText(studentModelRoot.getEmail());
                        if (studentModelRoot.getPhoneNumber().equals("")){
                            phoneRow.setVisibility(View.GONE);
                        }
                        if (studentModelRoot.getEmail().equals("")){
                            emailRow.setVisibility(View.GONE);
                        }
                        //Run through lessonMap to get attendance values
                        getPresentAndAbsent();
                        //Set values to pieChart
                        initPieWeek();
                        tvTotal.setText(String.valueOf(lessonMap.size()));
                        tvAbsent.setText(String.valueOf(absent));
                        tvPresent.setText(String.valueOf(present));
                        tvLate.setText(String.valueOf(late));
                        String percent;
                        if (present == 0) percent = "0%";
                        else {
                            DecimalFormat decimalFormat = new DecimalFormat("#.00");
                            percent= decimalFormat.format(1f*present/lessonMap.size()*100)+ "%";
                        }
                        tvPercent.setText(percent);

//                        if (studentModelRoot.getAvatar() != null){
//                            String avatarString = studentModelRoot.getAvatar();
//                            byte[] decodedString = Base64.decode(avatarString, Base64.URL_SAFE);
//                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
//                            avatar.setImageBitmap(decodedByte);
//                            imgAvatar.setImageBitmap(decodedByte);
//                        }
                        storageReference.child(user.getUid() + "/" + SUBJECTS_PATH + "/" +
                                        classRoot.getId() + "/" + studentModelRoot.getId() + "/avatar"+ "/" + "avatar.jpg").getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    Picasso.get().load(uri)
                                            .placeholder(R.drawable.img_item_class).error(R.drawable.img_item_class)
                                            .into(new Target() {
                                                @Override
                                                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                                    avatarStudentInfo = bitmap;
                                                    avatar.setImageBitmap(avatarStudentInfo);
                                                    imgAvatar.setImageBitmap(avatarStudentInfo);
                                                }

                                                @Override
                                                public void onBitmapFailed(Exception e, Drawable errorDrawable) {

                                                }

                                                @Override
                                                public void onPrepareLoad(Drawable placeHolderDrawable) {

                                                }
                                            });
                                });
                    }

                    getListEmbFace();
                });
    }

    //Run through lessonMap to get attendance values
    private void getPresentAndAbsent() {
        absent = 0;
        present = 0;
        late = 0;
        for (Map.Entry<String, Integer> set : lessonMap.entrySet()){
            int state = set.getValue();
            switch (state){
                case 0:
                    absent ++;
                    break;
                case 1:
                    present++;
                    break;
                case 2:
                default:
                    late ++;
            }
        }
    }

    private void setButtons() {
        btnEdit.setOnClickListener(view -> {
            //Initialize edit dialog
            Dialog editStudentDialog = new Dialog(this);
            editStudentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            editStudentDialog.setContentView(R.layout.dialog_edit_student);
            editStudentDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            editStudentDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            EditText etStudentName = editStudentDialog.findViewById(R.id.etStudentName);
            EditText etStudentLastName = editStudentDialog.findViewById(R.id.etStudentLastName);
            EditText etStudentID = editStudentDialog.findViewById(R.id.etStudentID);
            EditText etStudentPhone = editStudentDialog.findViewById(R.id.etStudentPhone);
            EditText etStudentEmail = editStudentDialog.findViewById(R.id.etStudentEmail);
            circleImageView = editStudentDialog.findViewById(R.id.profile_image_front);
            Button btnEdit = editStudentDialog.findViewById(R.id.btn_edit);
            Button btnCancel = editStudentDialog.findViewById(R.id.btn_cancel);
            ImageButton btnChangeAvatar = editStudentDialog.findViewById(R.id.btn_add_avatar_front);
            if(avatarStudentInfo != null)
                circleImageView.setImageBitmap(avatarStudentInfo);


            btnChangeAvatar.setOnClickListener(view1 -> {
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    if(Boolean.TRUE.equals(getFromPerf(this, "ALLOW_KEY"))) {
                        //Access setting
                        showSetting();
                    } else if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED
                            || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
                    }
                } else {
                    openDialogChangeAvatar(106, 107);
                }
            });

            String oldID = studentModelRoot.getId();
            if (studentModelRoot.getName() != null){
                etStudentName.setText(studentModelRoot.getName());
            }
            etStudentLastName.setText(studentModelRoot.getLastName());
            etStudentID.setText(oldID);
            etStudentPhone.setText(studentModelRoot.getPhoneNumber());
            etStudentEmail.setText(studentModelRoot.getEmail());

//            if (studentModelRoot.getAvatar() != null) {
//                String avatarString = studentModelRoot.getAvatar();
//                byte[] decodedString = Base64.decode(avatarString, Base64.URL_SAFE);
//                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
//                circleImageView.setImageBitmap(decodedByte);
//            }

            btnEdit.setOnClickListener(view1 -> {
                String newName = etStudentName.getText().toString().trim();
                String newLastName = etStudentLastName.getText().toString().trim();
                String newID = etStudentID.getText().toString().trim();
                String newPhone = etStudentPhone.getText().toString().trim();
                String newEmail = etStudentEmail.getText().toString().trim();
//                String avatar = null;
//                if(studentModelRoot.getAvatar() != null)
//                    avatar = studentModelRoot.getAvatar();
//                if(thumbnailFace != null) {
//                    avatar = getImageData(thumbnailFace);
//                    this.avatar.setImageBitmap(thumbnailFace);
//                }

                if(!oldID.equals(newID)) {
                    StorageReference attendance = FirebaseStorage.getInstance().getReference();
                    StorageReference faceData = FirebaseStorage.getInstance().getReference();
                    StorageReference avatar = FirebaseStorage.getInstance().getReference();
                    StorageReference newAttendance = FirebaseStorage.getInstance().getReference();
                    StorageReference newFaceData = FirebaseStorage.getInstance().getReference();
                    StorageReference newAvatar = FirebaseStorage.getInstance().getReference();

                    for(String itemID : lessonIdList) {
                        attendance.child(user.getUid() + "/" + SUBJECTS_PATH + "/" + classRoot.getId() +
                                        "/" + studentModelRoot.getId() + "/attendance/" + itemID + ".jpg").getBytes(500000)
                                .addOnCompleteListener(task -> {
                                    if(task.isSuccessful()) {
                                        newAttendance.child(user.getUid() + "/" + SUBJECTS_PATH + "/" + classRoot.getId() +
                                                        "/" + newID + "/attendance/" + itemID + ".jpg")
                                                .putBytes(task.getResult());
                                        FirebaseStorage.getInstance().getReference(user.getUid() + "/" + SUBJECTS_PATH + "/" + classRoot.getId() +
                                                "/" + studentModelRoot.getId() + "/attendance/" + itemID + ".jpg").delete();
                                    }
                                });
                    }

                    faceData.child(user.getUid() + "/" + SUBJECTS_PATH + "/" + classRoot.getId() +
                                    "/" + studentModelRoot.getId() + "/face_data")
                            .listAll().addOnCompleteListener(task -> {
                                int i = 0;
                                for (StorageReference sRFaceData : task.getResult().getItems()) {
                                    int finalI = i;
                                    sRFaceData.getBytes(500000).addOnCompleteListener(task1 -> {
                                        newFaceData.child(user.getUid() + "/" + SUBJECTS_PATH + "/" + classRoot.getId() +
                                                        "/" + newID + "/face_data/" + (System.currentTimeMillis() + finalI) + ".jpg")
                                                .putBytes(task1.getResult());
                                        sRFaceData.delete();
                                    });
                                    i++;
                                }
                            });

                    avatar.child(user.getUid() + "/" + SUBJECTS_PATH + "/" + classRoot.getId() +
                                    "/" + studentModelRoot.getId() + "/avatar")
                            .listAll().addOnCompleteListener(task -> {
                                for (StorageReference sRAvatar : task.getResult().getItems()) {
                                    sRAvatar.getBytes(500000).addOnCompleteListener(task1 -> {
                                        newAvatar.child(user.getUid() + "/" + SUBJECTS_PATH + "/" + classRoot.getId() +
                                                        "/" + newID + "/avatar/avatar.jpg")
                                                .putBytes(task1.getResult());
                                        sRAvatar.delete();
                                    });
                                }
                            });
                }

                boolean invalidName = TextUtils.isEmpty(newName);
                boolean invalidID = TextUtils.isEmpty(newID);
                boolean invalidEmail = !Patterns.EMAIL_ADDRESS.matcher(newEmail).matches();
                if (newEmail.equals("")){
                    invalidEmail = false;
                }
                boolean isNewID = checkUpdatedID(oldID, newID);
                //Check valid
                if (invalidName){
                    etStudentName.setError(getString(R.string.no_empty));
                    etStudentName.requestFocus();
                } else if (invalidID){
                    etStudentID.setError(getString(R.string.no_empty));
                    etStudentID.requestFocus();
                } else if (!isNewID){
                    etStudentID.setError(getString(R.string.no_empty));
                    etStudentID.requestFocus();
                    etStudentID.selectAll();
                    etStudentID.setSelection(newID.length());
                } else if (invalidEmail) {
                    etStudentEmail.setError(getString(R.string.haved_studentID));
                    etStudentEmail.requestFocus();
                    etStudentEmail.selectAll();
                    etStudentEmail.setSelection(newEmail.length());
                }else {
                    //Update in db
                    StudentModel updateStudent = new StudentModel();

                    updateStudent.setId(newID);
                    updateStudent.setName(newName);
                    if (!newLastName.equals("")){
                        updateStudent.setLastName(newLastName);
                    }
                    updateStudent.setEmail(newEmail);
                    updateStudent.setPhoneNumber(newPhone);
                    updateStudent.setEmbedding(studentModelRoot.getEmbedding());
//                    updateStudent.setAvatar(studentModelRoot.getAvatar());
                    updateStudent.setLessonMap(studentModelRoot.getLessonMap());
//                    embed.add(embedTemporary);
//                    updateStudent.setEmbedding(embed);
//                    updateStudent.setAvatar(avatar);
//
//                    ByteArrayOutputStream os = new ByteArrayOutputStream();
//                    thumbnailFace.compress(Bitmap.CompressFormat.JPEG, 100, os);
//                    String path = MediaStore.Images.Media.insertImage(getContentResolver(), thumbnailFace, System.currentTimeMillis()+"", "image");
//                    Uri uriFace = Uri.parse(path);
//
//                    StorageReference storageReference = FirebaseStorage.getInstance().getReference();
//                    StorageReference moutainImage = storageReference.child(user.getUid() + "/" + SUBJECTS_PATH + "/" +
//                            classRoot.getId() + "/" + updateStudent.getId() + "/" + System.currentTimeMillis() + ".jpg");
//                    moutainImage.putFile(uriFace).addOnCompleteListener(task -> {
//                        File fdelete = new File(getRealPathFromURI(this, uriFace));
//                        if (fdelete.exists())
//                            fdelete.delete();
//                        getStudentData();
//                    });

                    if(avatarStudent != null) {
                        avatarStudent = getResizedBitmapAvatar(avatarStudent, 500);

                        String path = MediaStore.Images.Media.insertImage(getContentResolver(), avatarStudent, System.currentTimeMillis()+"", "image");
                        Uri uriFace = Uri.parse(path);

                        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
                        StorageReference imgAvatar = storageReference.child(user.getUid() + "/" + SUBJECTS_PATH + "/" +
                                classRoot.getId() + "/" + updateStudent.getId() + "/avatar/avatar.jpg");
                        imgAvatar.putFile(uriFace).addOnCompleteListener(task -> {
                            File fdelete = new File(getRealPathFromURI(this, uriFace));
                            if (fdelete.exists())
                                fdelete.delete();
                            avatarStudent = null;
                            getStudentData();
                        });
                    }

                    if (!Objects.equals(oldID, newID)){
                        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId() + "")
                                .collection(STUDENTS_PATH).document(oldID)
                                .delete();
                    }
                    db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId() + "")
                            .collection(STUDENTS_PATH).document(updateStudent.getId())
                            .set(updateStudent).addOnSuccessListener(unused -> {
                                thumbnail = null;
                                thumbnailFace = null;
                            });

                    //Update root student
                    if (!newLastName.equals("")){
                        studentModelRoot.setLastName(newLastName);
                    }
                    studentModelRoot.setName(newName);
                    studentModelRoot.setId(newID);
                    studentModelRoot.setPhoneNumber(newPhone);
                    studentModelRoot.setEmail(newEmail);
                    //Update text view
                    String fullName;
                    if (!newLastName.equals("")){
                        fullName = studentModelRoot.getLastName() + " " + studentModelRoot.getName();
                    } else {
                        fullName = studentModelRoot.getName();
                    }
                    tvStudentName.setText(fullName);
                    tvStudentID.setText(studentModelRoot.getId());
                    if (newPhone.equals("")){
                        phoneRow.setVisibility(View.GONE);
                    } else {
                        tvStudentPhone.setText(studentModelRoot.getPhoneNumber());
                        phoneRow.setVisibility(View.VISIBLE);
                    }
                    if (newEmail.equals("")){
                        emailRow.setVisibility(View.GONE);
                    } else {
                        tvStudentEmail.setText(studentModelRoot.getEmail());
                        emailRow.setVisibility(View.VISIBLE);
                    }
                    setResult(RESULT_OK);

                    //Cancel dialog
                    editStudentDialog.cancel();
                }
            });

            //Cancel button
            btnCancel.setOnClickListener(view1 -> editStudentDialog.cancel());
            //Show deleteDialog
            editStudentDialog.show();
        });

        btnBack.setOnClickListener(view -> finish());
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

    private void openDialogChangeAvatar(int requescodeCamera, int requescodeGallery) {
        Dialog addAvatarDialog = new Dialog(this);
        addAvatarDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        addAvatarDialog.setContentView(R.layout.dialog_add_avatar);
        addAvatarDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addAvatarDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        LinearLayout btnAddAvatarCamera = addAvatarDialog.findViewById(R.id.btn_add_avatar_camera);
        LinearLayout btnAddAvatarGallery = addAvatarDialog.findViewById(R.id.btn_add_avatar_gallery);

        btnAddAvatarCamera.setOnClickListener(view3 -> {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, requescodeCamera);
            addAvatarDialog.dismiss();
        });

        btnAddAvatarGallery.setOnClickListener(view3 -> {
            Intent gallery = new Intent(Intent.ACTION_PICK);
            gallery.setType("image/*");
            startActivityForResult(gallery, requescodeGallery);
            addAvatarDialog.dismiss();
        });
        addAvatarDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 101) {
            checkPermission(grantResults);
        }
    }

    private void checkPermission(int[] grantResults) {
        int x = 0;
        for(int permission : grantResults) {
            if(permission == PackageManager.PERMISSION_DENIED)
                x++;
        }
//        if(x == 0) {
//            openDialogChangeAvatar();
//        } else {
        if(x != 0) {
            boolean showRotinable = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA);
            boolean showRotinable2 = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if(!showRotinable && !showRotinable2) {
                saveToPreferences(this, "ALLOW_KEY", true);
            }
        }
    }

    private void showSetting() {
        new AlertDialog.Builder(this)
                .setTitle("Thông báo quyền truy cập!!!")
                .setMessage("Tính năng này yêu cầu quyền truy cập vào máy ảnh và bộ nhớ, vui lòng cấp quyền này trong Cài đặt.") //This feature requires camera and storage access permission, please grant this permission in Settings.
                .setNegativeButton("Hủy", (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton("Chấp nhận", (dialogInterface, i) -> {
                    Intent accessSetting = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    accessSetting.setData(uri);
                    startActivityForResult(accessSetting, 103);
                }).show();
    }
    //Check trạng thái permission
    private static Boolean getFromPerf(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences("ALLOW_KEY", Context.MODE_PRIVATE);
        return preferences.getBoolean(key, false);
    }

    //Lưu trạng thái permisssion
    private static void saveToPreferences(Context context, String key, Boolean allowed) {
        SharedPreferences preferences = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("ALLOW_KEY", allowed);
        editor.commit();
    }

    //This function returns true if updated id is not in studentList and different from oldID
    private boolean checkUpdatedID(String oldID, String newID) {
        boolean isNewID = true;
        for (String studentId1 : studentId){
            if (!newID.equals(oldID) && newID.equals(studentId1)){
                isNewID = false;
                break;
            }
        }
        return isNewID;
    }
    //Set values to pieChart
    private void initPieWeek() {
        int[] week = {present, absent, late};
        PieCharAnalysis.initPieStatic(this, week);
        PieCharAnalysis.getStatistical(chartStudentSub);
        PieCharAnalysis.setCirleWidth(chartStudentSub, 65, true);
    }


    //Add lessons to list
    private CompletableFuture<Boolean> initList() {
        infoClasses.clear();
        DateFormat format = new SimpleDateFormat(getString(R.string.fomat_dmy), Locale.getDefault());
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        AtomicBoolean isSuccess = new AtomicBoolean(true);
        AtomicInteger stack = new AtomicInteger(lessonModels.size());
        for(int i = 0; i < lessonModels.size(); i++) {
            LessonModel lessonModel = lessonModels.get(i);
            String lessonDate = format.format(lessonModel.getDayTime());
            int state = lessonMap.get(String.valueOf(lessonModel.getId()));

            DateFormat formatTime = new SimpleDateFormat("HH:mm");
            Date dateStart = new Date(lessonModel.getStartTime());
            int index = i;
            storageReference.child(user.getUid() + "/" + SUBJECTS_PATH + "/" +
                    classRoot.getId() + "/" + studentRootID + "/attendance/" + lessonModel.getId() + ".jpg").getDownloadUrl()
                    .addOnCompleteListener(task -> {
                        Uri faceUri = null;
                        if (task.isSuccessful()){
                            faceUri = task.getResult();
                        }
                        infoClasses.add(new StudentSubClassList(state,(index < 9) ? ("0"+ (index +1)) : ((index +1)+""),
                                formatTime.format(dateStart), lessonDate, faceUri));
                        infoClasses.sort(((studentSubClassList, t1) -> studentSubClassList.getSerial().compareToIgnoreCase(t1.getSerial())));
                        if (stack.decrementAndGet() < 1) {
                            future.complete(isSuccess.get());
                        }
                    });
        }
        return future;
    }

    private void updateListAttendance() {
        StudentSubClassListAdapter studentSubClassListAdapter = new StudentSubClassListAdapter(infoClasses, this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(studentSubClassListAdapter);
        studentSubClassListAdapter.updateList(infoClasses);
    }

    private void ORM(){
        btnBack = findViewById(R.id.backS);
        btnEdit = findViewById(R.id.edtBtn);
        btnAddDataFace = findViewById(R.id.btn_add_data_face);
        chartStudentSub = findViewById(R.id.progressBarSub);
        recyclerView = findViewById(R.id.list_info_student);
        tvStudentName = findViewById(R.id.tv_student_name);
        tvStudentID = findViewById(R.id.tv_student_id);
        tvStudentPhone = findViewById(R.id.tv_student_phone);
        tvStudentEmail = findViewById(R.id.tv_student_email);
        emailRow = findViewById(R.id.emailRow);
        phoneRow = findViewById(R.id.phoneRow);
        avatar = findViewById(R.id.profile_image);
    }

    @Override
    public void onClick(int position) {
        Bundle bundle = new Bundle();
        LessonModel lessonModel = lessonModels.get(position);
        bundle.putSerializable("lessonModel",lessonModel);
        bundle.putSerializable("class", classRoot);
        Intent intent = new Intent(this, Attendance.class);
        intent.putExtras(bundle);
        startActivityForResult(intent, 105);
    }

    @Override
    public void deleteItem(int position) {
        mountains.listAll().addOnSuccessListener(e -> {
            e.getItems().get(position).delete();
            uri.remove(position);
            studentModelRoot.getEmbedding().remove(position).clear();

            if(studentModelRoot.getEmbedding().size() == 0)
                db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId() + "").collection(STUDENTS_PATH)
                        .document(studentModelRoot.getId() + "").update("embedding", null);
            else
                db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId() + "").collection(STUDENTS_PATH)
                    .document(studentModelRoot.getId() + "").update("embedding", studentModelRoot.getEmbedding());
            adapter.setList(uri);
            view.setAdapter(adapter);

            getStudentData();
        });
    }

    @Override
    public void editItem(int position) {

    }

    @Override
    public void getData(String[] listData, String startTime, String endTime, String address, String room) {

    }
}