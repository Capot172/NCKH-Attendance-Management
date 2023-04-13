package com.atechclass.attendance.function.class_list.Student;

import static android.app.Activity.RESULT_OK;
import static com.bumptech.glide.load.resource.bitmap.TransformationUtils.rotateImage;
import static com.atechclass.attendance.ultis.HandleAvatar.activity;
import static com.atechclass.attendance.ultis.HandleAvatar.getCropBitmapByCPU;
import static com.atechclass.attendance.ultis.HandleAvatar.getCropBitmapByCPUAvatar;
import static com.atechclass.attendance.ultis.HandleAvatar.getEmb;
import static com.atechclass.attendance.ultis.HandleAvatar.getResizedBitmap;
import static com.atechclass.attendance.ultis.HandleAvatar.getResizedBitmapAvatar;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
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
import android.graphics.drawable.InsetDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atechclass.attendance.Language;
import com.atechclass.attendance.R;
import com.atechclass.attendance.adapter.FaceEmbCreateAdapter;
import com.atechclass.attendance.adapter.StudentRecyclerViewAdapter;
import com.atechclass.attendance.databinding.DialogImportExcelBinding;
import com.atechclass.attendance.databinding.DialogImportStudentsBinding;
import com.atechclass.attendance.interfaces.IOnClickStudent;
import com.atechclass.attendance.interfaces.OnClickItem;
import com.atechclass.attendance.model.ClassModel;
import com.atechclass.attendance.model.DataFace;
import com.atechclass.attendance.model.LessonModel;
import com.atechclass.attendance.model.StudentModel;
import com.atechclass.attendance.ultis.UserLogin;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.squareup.picasso.Picasso;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;

public class ListStudents extends Fragment implements IOnClickStudent, OnClickItem {

    private static final String SUBJECTS_PATH = "Subjects";
    private static final String STUDENTS_PATH = "Students";
    private static final String USER_PATH = "User";
    private static final String LESSON_PATH = "Lesson";
    private Language language;

    private ClassModel classRoot;

    private LinearLayout noStudentLayout;

    private RecyclerView recyclerView;
    private RecyclerView listDataFaces;

    private StudentRecyclerViewAdapter adapter;
    private FaceEmbCreateAdapter faceEmbCreateAdapter;

    private final List<StudentModel> studentList = new ArrayList<>();
    private final ArrayList<String> lessonIdList = new ArrayList<>();
    private final ArrayList<String> studentId = new ArrayList<>();
    private final List<DataFace> dataFaces = new ArrayList<>();
    private final List<Map<String, Float>> embed = new ArrayList<>();

    private SearchView searchView;
    private TextView tvStudentNumber;
    private FloatingActionButton fab;
    private FloatingActionButton fabImport;
    private FloatingActionButton fabAdd;
    private final String modelFile="mobile_face_net.tflite"; //model name

    private final int INPUT_SIZE = 112;
    private int[] intValues;
    private final float IMAGE_MEAN = 128.0f;
    private final float IMAGE_STD = 128.0f;
    private final int OUTPUT_SIZE=192; //Output size of model

    private Interpreter tfLite;
    private FaceDetector detector;
    private Bitmap thumbnail = null;
    private Bitmap avatar = null;
    private Intent intent;
    private CircleImageView imgAvatar;
    private CircleImageView circleImageViewAvatar;

    private Uri uriDataFace;
    private Uri imageUri;

    public static StorageReference storageReference = FirebaseStorage.getInstance().getReference();

    private FirebaseFirestore db;
    private FirebaseUser user;

    private boolean isOpen = false;
    private boolean hasChanged = false;

    private View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        language = new Language(getActivity());
        language.Language();
        //Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_list_students, container, false);
        FirebaseAuth mAuth = UserLogin.getAuth();
        db = FirebaseFirestore.getInstance();
        activity = getActivity();

        if (getArguments() != null) {
            classRoot = (ClassModel) getArguments().getSerializable("class");
        }
        //Set for tfLite
        try {
            tfLite=new Interpreter(loadModelFile(getActivity(), modelFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

        user = mAuth.getCurrentUser();
        if (user != null) {
            getLessonDataFromDatabase();
            getStudentsDataFromDatabase(view);
        }

        //Call back for refreshing this page
        setFragmentResultListeners();

        initWidgets();

        setSearchView();

        setFAB();
        importStudents();

        setStudentRecyclerView();

        return view;
    }

    private void setFragmentResultListeners() {
        requireActivity().getSupportFragmentManager().setFragmentResultListener("attendance", this, (requestKey, bundle) -> {
            boolean result = bundle.getBoolean("hasChanged");
            if (result){
                getLessonDataFromDatabase();
                getStudentsDataFromDatabase(view);
            }
        });

        requireActivity().getSupportFragmentManager().setFragmentResultListener("openAdd", this, (requestKey, bundle) -> {
            boolean result = bundle.getBoolean("openAdd");
            if (result){
                getLessonDataFromDatabase();
                new Handler(Looper.getMainLooper()).postDelayed((() -> fab.callOnClick()), 500);
                new Handler(Looper.getMainLooper()).postDelayed((() -> fabAdd.callOnClick()), 800);
            }
        });

        requireActivity().getSupportFragmentManager().setFragmentResultListener("create", this, (requestKey, bundle) -> {
            boolean result = bundle.getBoolean("hasChanged");
            if (result){
                getLessonDataFromDatabase();
                getStudentsDataFromDatabase(view);
            }
        });

        requireActivity().getSupportFragmentManager().setFragmentResultListener("delete", this, (requestKey, bundle) -> {
            boolean result = bundle.getBoolean("hasChanged");
            if (result){
                getLessonDataFromDatabase();
                getStudentsDataFromDatabase(view);
            }
        });
    }

    private MappedByteBuffer loadModelFile(Activity activity, String MODEL_FILE) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void importStudents() {
        fabImport.setOnClickListener(view -> {
            Dialog dialog = new Dialog(getActivity());
            dialog.show();
            DialogImportStudentsBinding importStudentsBinding = DataBindingUtil.inflate(LayoutInflater.from(getActivity()), R.layout.dialog_import_students, null, false);
            dialog.setContentView(importStudentsBinding.getRoot());
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            importStudentsBinding.btnImportAccept.setOnClickListener(view1 -> {
                if(importStudentsBinding.rdbFromClipboard.isChecked()) {
                    ClipboardManager clipboard  = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    if(clipboard.getPrimaryClip() != null) {
                        ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                        String data = item.getText().toString();
                        Intent intent = new Intent(getContext(), ImportStudents.class);
                        intent.putExtra("data", data);
                        intent.putExtra("idClass", classRoot.getId()+"");
                        Bundle bundle = new Bundle();
                        bundle.putStringArrayList("studentId", studentId);
                        bundle.putStringArrayList("idLessons", lessonIdList);
                        intent.putExtras(bundle);
                        startActivityForResult(intent, 1);
                    } else {
                        Toast.makeText(getActivity(), "No data", Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                } else if(importStudentsBinding.rdbFromExcel.isChecked()) {
                    dialog.dismiss();
                    Dialog dialogExcel = new Dialog(getActivity());
                    dialogExcel.show();
                    DialogImportExcelBinding importExcelBinding = DataBindingUtil.inflate(LayoutInflater.from(getActivity()), R.layout.dialog_import_excel, null, false);
                    dialogExcel.setContentView(importExcelBinding.getRoot());
                    ColorDrawable back = new ColorDrawable(Color.TRANSPARENT);
                    InsetDrawable inset = new InsetDrawable(back, 100);
                    dialogExcel.getWindow().setBackgroundDrawable(inset);
                    dialogExcel.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                    importExcelBinding.btnExcelUnderstand.setOnClickListener(view2 -> dialogExcel.dismiss());
                }
            });

            importStudentsBinding.btnImportDeniel.setOnClickListener(view1 -> dialog.dismiss());
            animateFabHide();
        });
    }

    private void getStudentsDataFromDatabase(View view) {
        view.findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId() + "").collection(STUDENTS_PATH)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        //Clear list
                        studentList.clear();
                        studentId.clear();
                        for (QueryDocumentSnapshot studentSnapshot : task.getResult()) {
                            StudentModel studentModel = studentSnapshot.toObject(StudentModel.class);
                            studentList.add(studentModel);
                            studentId.add(studentModel.getId());
                        }
                        //Update
                        updateRecyclerView();
                        //If there is no student, show no student layout
                        if (task.getResult().isEmpty()) {
                            noStudentLayout.setVisibility(View.VISIBLE);
                        } else {
                            noStudentLayout.setVisibility(View.GONE);
                        }
                        view.findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                    }
                });
    }

    private void getLessonDataFromDatabase() {
        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId()+"").collection(LESSON_PATH)
                .get().addOnCompleteListener(task -> {
                   if (task.isSuccessful()){
                       lessonIdList.clear();
                       if (task.getResult().isEmpty()) return;
                       for (QueryDocumentSnapshot lessonSnapshot : task.getResult()) {
                           LessonModel lessonModel = lessonSnapshot.toObject(LessonModel.class);
                           lessonIdList.add(lessonModel.getId()+"");
                       }
                   }
                });
    }

    private void setFAB() {
        fab.setOnClickListener(view1 -> {
            if(!isOpen){
                //Open fab
                animateFabShow();
                //Hide fab after 10s
                new Handler(Looper.getMainLooper()).postDelayed(this::animateFabHide, 10000);
            } else{
                animateFabHide();
            }
        });
        setFabAdd();
    }

    private void setFabAdd() {
        fabAdd.setOnClickListener(view1 -> {
            //Hide fab
            animateFabHide();
            //Initialize create dialog
            Dialog addDialog = new Dialog(getContext());
            addDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            addDialog.setContentView(R.layout.dialog_add_student);
            addDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            addDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            EditText etStudentName = addDialog.findViewById(R.id.etStudentName);
            EditText etStudentLastName = addDialog.findViewById(R.id.etStudentLastName);
            EditText etStudentID = addDialog.findViewById(R.id.etStudentID);
            EditText etStudentPhone = addDialog.findViewById(R.id.etStudentPhone);
            EditText etStudentEmail = addDialog.findViewById(R.id.etStudentEmail);
            ImageButton btnAddAvatar = addDialog.findViewById(R.id.btn_add_avatar);
            ImageButton btnAddFaceData = addDialog.findViewById(R.id.btn_add_data_face);
            imgAvatar = addDialog.findViewById(R.id.profile_image);
            listDataFaces = addDialog.findViewById(R.id.list_data_face);
            Button btnAdd = addDialog.findViewById(R.id.btn_add);

            btnAddFaceData.setOnClickListener(view2 -> {
                if(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    if(getFromPerf(getContext(), "ALLOW_KEY")) {
                        //Access setting
                        showSetting();
                    } else if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED
                            || ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
                    }
                } else {
                    openDialogChangeAvatar(103, 104);
                }
            });
            
            btnAddAvatar.setOnClickListener(view2 -> {
                if(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    if(getFromPerf(getContext(), "ALLOW_KEY")) {
                        //Access setting
                        showSetting();
                    } else if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED
                            || ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
                    }
                } else {
                    openDialogChangeAvatar(105, 106);
                }
            });

            btnAdd.setOnClickListener(view2 -> {
                String name = etStudentName.getText().toString().trim();
                String lastName = etStudentLastName.getText().toString().trim();
                String id = etStudentID.getText().toString().trim();
                String phone = etStudentPhone.getText().toString().trim();
                String email = etStudentEmail.getText().toString().trim();
//                String avatar = null;
//                if(thumbnailFace != null) {
//                    avatar = getImageData(thumbnailFace);
//                }

                boolean invalidName = TextUtils.isEmpty(name);
                boolean invalidID = TextUtils.isEmpty(id);
                boolean invalidEmail = !Patterns.EMAIL_ADDRESS.matcher(email).matches();
                if (email.length() == 0){
                    invalidEmail = false;
                }
                //Check if there are any student with this new id
                boolean newId = isNewId(id);

                if (invalidName){
                    etStudentName.setError(getString(R.string.no_empty));
                    etStudentName.requestFocus();
                } else if (invalidID){
                    etStudentID.setError(getString(R.string.no_empty));
                    etStudentID.requestFocus();
                } else if (!newId){
                    etStudentID.setError(getString(R.string.haved_studentID));
                    etStudentID.requestFocus();
                    etStudentID.selectAll();
                    etStudentID.setSelection(id.length());
                } else if (invalidEmail){
                    etStudentEmail.setError(getString(R.string.invalid_email));
                    etStudentEmail.requestFocus();
                    etStudentEmail.selectAll();
                    etStudentEmail.setSelection(email.length());
                } else {
                    if (studentList.isEmpty()){
                        noStudentLayout.setVisibility(View.GONE);
                    }

                    StudentModel studentModel = new StudentModel();

                    studentModel.setId(id);
                    if (!lastName.equals(""))
                        studentModel.setLastName(lastName);
                    studentModel.setName(name);
                    studentModel.setPhoneNumber(phone);
                    studentModel.setEmail(email);
//                    studentModel.setAvatar(avatar);
//                    if(embed.size() > 0)
//                        studentModel.setEmbedding(embed);

                    if(dataFaces.size() > 0) {
                        for(int i = 0; i < dataFaces.size(); i++) {
                            embed.add(dataFaces.get(i).getEmb());
                            uriDataFace = Uri.parse(dataFaces.get(i).getPath());
                            StorageReference srDataFace = storageReference.child(user.getUid() + "/" + SUBJECTS_PATH + "/" +
                                classRoot.getId() + "/" + studentModel.getId() + "/face_data/" + (System.currentTimeMillis() + i) + ".jpg");

                            srDataFace.putFile(uriDataFace).addOnCompleteListener(task -> {
                                File fdelete = new File(getRealPathFromURI(getContext(), uriDataFace));
                                if (fdelete.exists()) {
                                    fdelete.delete();
                                }
                            });
                        }
                        dataFaces.clear();
                        studentModel.setEmbedding(embed);
                    }

                    //Update lesson database
                    db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId()+"")
                            .collection(LESSON_PATH).get().addOnCompleteListener(task -> {
                                if (task.isSuccessful()){
                                    for (QueryDocumentSnapshot lessonSnap : task.getResult()){
                                        LessonModel lessonModel = lessonSnap.toObject(LessonModel.class);
                                        if (lessonModel.getAbsent() + lessonModel.getLate() + lessonModel.getPresent() != 0){
                                            db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId()+"")
                                                    .collection(LESSON_PATH).document(lessonModel.getId() +"").update("absent", FieldValue.increment(1));
                                        }
                                    }
                                    //Update lesson fragment
                                    hasChanged = true;
                                    Bundle result = new Bundle();
                                    result.putBoolean("hasChanged", hasChanged);
                                    requireActivity().getSupportFragmentManager().setFragmentResult("result", result);
                                }
                            });

                    Map<String, Integer> map = new HashMap<>();
                    for (String lesson : lessonIdList){
                        map.put(lesson, 0);
                    }
                    studentModel.setLessonMap(map);

                    db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId() + "")
                            .collection(STUDENTS_PATH).document(studentModel.getId()).set(studentModel).addOnSuccessListener(unused -> embed.clear());

//                    if(thumbnailFace != null) {
//                        String path = MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), thumbnailFace, System.currentTimeMillis() + "", "image");
//                        Uri uri = Uri.parse(path);
//
//                        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
//                        StorageReference moutainImage = storageReference.child(user.getUid() + "/" + SUBJECTS_PATH + "/" +
//                                classRoot.getId() + "/" + studentModel.getId() + "/" + System.currentTimeMillis() + ".jpg");
//
//                        moutainImage.putFile(uri).addOnCompleteListener(task -> {
//                            File fdelete = new File(getRealPathFromURI(getContext(), uri));
//                            if (fdelete.exists()) {
//                                fdelete.delete();
//                            }
//                            thumbnail = null;
//                            thumbnailFace = null;
//                        });
//                    }


                    if(avatar != null) {

//                        avatar = Bitmap.createBitmap(avatar.getWidth(), avatar.getHeight(), Bitmap.Config.RGB_565);
//                        Canvas canvas = new Canvas(avatar);
//                        canvas.drawColor(Color.WHITE);
//                        canvas.drawBitmap(avatar, 0, 0, null);
//                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//                        avatar.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
                        avatar = getResizedBitmapAvatar(avatar, 500);

                        String path = MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), avatar, System.currentTimeMillis() + "", "image");
                        Uri uri = Uri.parse(path);

                        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
                        StorageReference moutainImage = storageReference.child(user.getUid() + "/" + SUBJECTS_PATH + "/" +
                                classRoot.getId() + "/" + studentModel.getId() + "/avatar/" + "avatar.jpg");

                        moutainImage.putFile(uri).addOnCompleteListener(task -> {
                            File fdelete = new File(getRealPathFromURI(getContext(), uri));
                            if (fdelete.exists()) {
                                fdelete.delete();
                            }
                            avatar = null;
                            getStudentsDataFromDatabase(view);
                            //Update
                            updateRecyclerView();
                        });
                    } else {
                        getStudentsDataFromDatabase(view);
                        //Update
                        updateRecyclerView();
                    }

                    studentList.add(studentModel);
                    studentId.add(studentModel.getId());
                    //Update lesson fragment
                    hasChanged = true;
                    Bundle result = new Bundle();
                    result.putBoolean("hasChanged", hasChanged);
                    //Call back for refreshing lessons fragment
                    requireActivity().getSupportFragmentManager().setFragmentResult("students", result);
                    //Cancel createDialog
                    addDialog.cancel();
                }

            });
            //Show createDialog
            addDialog.show();
        });
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

    private void showSetting() {
        new AlertDialog.Builder(getContext())
                .setTitle("Thông báo quyền truy cập!!!")
                .setMessage("Tính năng này yêu cầu quyền truy cập vào máy ảnh và bộ nhớ, vui lòng cấp quyền này trong Cài đặt.") //This feature requires camera and storage access permission, please grant this permission in Settings.
                .setNegativeButton("Hủy", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setPositiveButton("Chấp nhận", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent accessSetting = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                        accessSetting.setData(uri);
                        startActivityForResult(accessSetting, 103);
                    }
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
        if(x > 0) {
            boolean showRotinable = ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.CAMERA);
            boolean showRotinable2 = ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if(!showRotinable && !showRotinable2) {
                saveToPreferences(getContext(), "ALLOW_KEY", true);
            }
        }
    }

    private void openDialogChangeAvatar(int requescodeCamera, int requescodeGallery) {
        Dialog addAvatarDialog = new Dialog(getContext());
        addAvatarDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        addAvatarDialog.setContentView(R.layout.dialog_add_avatar);
        addAvatarDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addAvatarDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        LinearLayout btnAddAvatarCamera = addAvatarDialog.findViewById(R.id.btn_add_avatar_camera);
        LinearLayout btnAddAvatarGallery = addAvatarDialog.findViewById(R.id.btn_add_avatar_gallery);

        btnAddAvatarCamera.setOnClickListener(view3 -> {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            imageUri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
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
    public void onClick(StudentModel studentModel) {
        Bundle bundle = new Bundle();
        bundle.putString("student", studentModel.getId());
        bundle.putSerializable("class", classRoot);
        bundle.putStringArrayList("studentId", studentId);
        Intent intent = new Intent(getContext(), StudentInfo.class);
        intent.putExtras(bundle);
        startActivityForResult(intent, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK){
            getStudentsDataFromDatabase(view);
            //Update lesson fragment
            hasChanged = true;
            Bundle result = new Bundle();
            result.putBoolean("hasChanged", hasChanged);
            requireActivity().getSupportFragmentManager().setFragmentResult("attendanceFromInfo", result);
        }
        if(requestCode == 103 && resultCode == RESULT_OK && imageUri != null) {
            try {
                thumbnail = MediaStore.Images.Media.getBitmap(
                        getActivity().getContentResolver(), imageUri);
                thumbnail = getImageRotation(thumbnail, imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            handleImage(thumbnail);
        }
        if(requestCode == 104 && data != null && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            try {
                thumbnail = MediaStore.Images.Media.getBitmap(
                        getActivity().getContentResolver(), uri);
                thumbnail = getImageRotation(thumbnail, uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            handleImage(thumbnail);
        }
        if(requestCode == 105 && resultCode == RESULT_OK && imageUri != null) {
            try {
                avatar = MediaStore.Images.Media.getBitmap(
                        getActivity().getContentResolver(), imageUri);
                avatar = getImageRotation(avatar, imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(imgAvatar != null)
                imgAvatar.setImageBitmap(avatar);
            if(circleImageViewAvatar != null)
                circleImageViewAvatar.setImageBitmap(avatar);
        }
        if(requestCode == 106 && data != null && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            try {
                avatar = MediaStore.Images.Media.getBitmap(
                        getActivity().getContentResolver(), uri);
                avatar = getImageRotation(avatar, uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(imgAvatar != null)
                imgAvatar.setImageBitmap(avatar);
            if(circleImageViewAvatar != null)
                circleImageViewAvatar.setImageBitmap(avatar);
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
                    if(faces.size() > 0) {
                        final RectF boundingBoxAvatar = new RectF(faces.get(0).getBoundingBox());
                        final RectF boundingBoxFace = new RectF(faces.get(0).getBoundingBox());

                        if (boundingBoxFace != null) {
                            //Lấy dữ liệu hộp của gương mặt
                            Bitmap cropped_face_avatar = getCropBitmapByCPUAvatar(thumbnail, boundingBoxAvatar);
                            //resize lại theo định dạng 112*112
                            Bitmap scaledAvatar = getResizedBitmap(cropped_face_avatar, 256, 256);
                            //Lấy dữ liệu hộp của gương mặt
                            Bitmap cropped_face = getCropBitmapByCPU(thumbnail, boundingBoxFace);
                            //resize lại theo định dạng 112*112
                            Bitmap scaledFace = getResizedBitmap(cropped_face, 112, 112);
//                            if(imgAvatar != null)
//                                imgAvatar.setImageBitmap(scaledAvatar);
//                            if(circleImageViewAvatar != null)
//                                circleImageViewAvatar.setImageBitmap(scaledAvatar);
//                            thumbnail = scaledFace;
//                            thumbnailFace = scaledAvatar;
                            getEmbFace(scaledFace, scaledAvatar);
                        }
                    }
                });
    }

    private void getEmbFace(Bitmap bitmap, Bitmap scaledAvatar) {
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

        float[][] embeedings = new float[1][OUTPUT_SIZE];
        outputMap.put(0, embeedings);
//        embed.clear();
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);
//        embed.add(getEmb(embeedings));
        String path = MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), scaledAvatar, System.currentTimeMillis() + "", "image");
        dataFaces.add(new DataFace(getEmb(embeedings), scaledAvatar, path));
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        faceEmbCreateAdapter = new FaceEmbCreateAdapter(this);
        faceEmbCreateAdapter.setList(dataFaces);
        listDataFaces.setLayoutManager(layoutManager);
        listDataFaces.setAdapter(faceEmbCreateAdapter);
    }

    private Bitmap getImageRotation(Bitmap img, Uri uri) throws IOException {
        InputStream input = getActivity().getContentResolver().openInputStream(uri);
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

    @Override
    public void onClickPopMenu(StudentRecyclerViewAdapter.StudentViewHolder holder, StudentModel studentModel, int position, Uri avatarImage) {
        //Initialize popup menu
        holder.popupMenu = new PopupMenu(getContext(), holder.itemView.findViewById(R.id.btn_edit));
        holder.popupMenu.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        holder.popupMenu.getMenuInflater().inflate(R.menu.menu_popup, holder.popupMenu.getMenu());
        MenuCompat.setGroupDividerEnabled(holder.popupMenu.getMenu(), true);
        //Pop up menu on click
        holder.popupMenu.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.menu_edit){
                //Initialize edit dialog
                Dialog editStudentDialog = new Dialog(getContext());
                editStudentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                editStudentDialog.setContentView(R.layout.dialog_edit_student);
                editStudentDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                editStudentDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                EditText etStudentName = editStudentDialog.findViewById(R.id.etStudentName);
                EditText etStudentLastName = editStudentDialog.findViewById(R.id.etStudentLastName);
                EditText etStudentID = editStudentDialog.findViewById(R.id.etStudentID);
                EditText etStudentPhone = editStudentDialog.findViewById(R.id.etStudentPhone);
                EditText etStudentEmail = editStudentDialog.findViewById(R.id.etStudentEmail);
                circleImageViewAvatar = editStudentDialog.findViewById(R.id.profile_image_front);
                Button btnEdit = editStudentDialog.findViewById(R.id.btn_edit);
                Button btnCancel = editStudentDialog.findViewById(R.id.btn_cancel);
                ImageButton btnChangeAvatar = editStudentDialog.findViewById(R.id.btn_add_avatar_front);

                if (avatarImage != null)
                    Picasso.get().load(avatarImage)
                        .placeholder(R.drawable.img_item_class).error(R.drawable.img_item_class)
                        .into(circleImageViewAvatar);
                else{
                    Drawable drawable = AppCompatResources.getDrawable(requireContext(), R.drawable.img_item_class);
                    circleImageViewAvatar.setImageDrawable(drawable);
                }


                btnChangeAvatar.setOnClickListener(view -> {
                    if(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED
                            || ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        if(getFromPerf(getContext(), "ALLOW_KEY")) {
                            //Access setting
                            showSetting();
                        } else if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED
                                || ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
                        }
                    } else {
                        openDialogChangeAvatar(105, 106);
                    }
                });

                String oldID = studentModel.getId();

                etStudentName.setText(studentModel.getName());
                if (studentModel.getLastName() != null)
                    etStudentLastName.setText(studentModel.getLastName());
                etStudentID.setText(oldID);
                etStudentPhone.setText(studentModel.getPhoneNumber());
                etStudentEmail.setText(studentModel.getEmail());

//                if (studentModel.getAvatar() != null) {
//                    String avatarString = studentModel.getAvatar();
//                    byte[] decodedString = Base64.decode(avatarString, Base64.URL_SAFE);
//                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
//                    circleImageViewAvatar.setImageBitmap(decodedByte);
//                }

                btnEdit.setOnClickListener(view1 -> {

                    String newName = etStudentName.getText().toString().trim();
                    String newLastName = etStudentLastName.getText().toString().trim();
                    String newID = etStudentID.getText().toString().trim();
                    String newPhone = etStudentPhone.getText().toString().trim();
                    String newEmail = etStudentEmail.getText().toString().trim();
//                    String avatar = null;
//                    if(thumbnailFace != null) {
//                        avatar = getImageData(thumbnailFace);
//                    }

                    if(!oldID.equals(newID)) {
                        StorageReference attendance = FirebaseStorage.getInstance().getReference();
                        StorageReference faceData = FirebaseStorage.getInstance().getReference();
                        StorageReference avatar = FirebaseStorage.getInstance().getReference();
                        StorageReference newAttendance = FirebaseStorage.getInstance().getReference();
                        StorageReference newFaceData = FirebaseStorage.getInstance().getReference();
                        StorageReference newAvatar = FirebaseStorage.getInstance().getReference();

                        for(String itemID : lessonIdList) {
                            attendance.child(user.getUid() + "/" + SUBJECTS_PATH + "/" + classRoot.getId() +
                                            "/" + studentModel.getId() + "/attendance/" + itemID + ".jpg").getBytes(500000)
                                    .addOnCompleteListener(task -> {
                                        if(task.isSuccessful()) {
                                            newAttendance.child(user.getUid() + "/" + SUBJECTS_PATH + "/" + classRoot.getId() +
                                                            "/" + newID + "/attendance/" + itemID + ".jpg")
                                                    .putBytes(task.getResult());
                                            FirebaseStorage.getInstance().getReference(user.getUid() + "/" + SUBJECTS_PATH + "/" + classRoot.getId() +
                                                    "/" + studentModel.getId() + "/attendance/" + itemID + ".jpg").delete();
                                        }
                                    });
                        }

                        faceData.child(user.getUid() + "/" + SUBJECTS_PATH + "/" + classRoot.getId() +
                                        "/" + studentModel.getId() + "/face_data")
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
                                        "/" + studentModel.getId() + "/avatar")
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

                    if (invalidName){
                        etStudentName.setError(getString(R.string.no_empty));
                        etStudentName.requestFocus();
                    } else if (invalidID){
                        etStudentID.setError(getString(R.string.no_empty));
                        etStudentID.requestFocus();
                    } else if (!isNewID){
                        etStudentID.setError(getString(R.string.haved_studentID));
                        etStudentID.requestFocus();
                        etStudentID.selectAll();
                        etStudentID.setSelection(newID.length());
                    } else if (invalidEmail) {
                        etStudentEmail.setError(getString(R.string.invalid_email));
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
                        updateStudent.setEmbedding(studentModel.getEmbedding());
//                        updateStudent.setAvatar(studentModel.getAvatar());
                        updateStudent.setLessonMap(studentModel.getLessonMap());
//                        updateStudent.setAvatar(avatar);

                        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId() + "")
                                .collection(STUDENTS_PATH).document(oldID)
                                .delete();
                        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId() + "")
                                .collection(STUDENTS_PATH).document(updateStudent.getId())
                                .set(updateStudent);
//                        if(thumbnailFace != null) {
//                            String path = MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), thumbnailFace, System.currentTimeMillis() + "", "image");
//                            Uri uri = Uri.parse(path);
//
//                            StorageReference storageReference = FirebaseStorage.getInstance().getReference();
//                            StorageReference moutainImage = storageReference.child(user.getUid() + "/" + SUBJECTS_PATH + "/" +
//                                    classRoot.getId() + "/" + studentModel.getId() + "/" + System.currentTimeMillis() + ".jpg");
//
//                            moutainImage.putFile(uri).addOnCompleteListener(task -> {
//                                File fdelete = new File(getRealPathFromURI(getContext(), uri));
//                                if (fdelete.exists()) {
//                                    fdelete.delete();
//                                }
//                                thumbnail = null;
//                                thumbnailFace = null;
//                            });
//                        }
                        if(avatar != null) {
                            avatar = getResizedBitmapAvatar(avatar, 500);

                            String path = MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), avatar, System.currentTimeMillis() + "", "image");
                            Uri uri = Uri.parse(path);

                            StorageReference moutainImage = storageReference.child(user.getUid() + "/" + SUBJECTS_PATH + "/" +
                                    classRoot.getId() + "/" + studentModel.getId() + "/avatar/avatar.jpg");

                            moutainImage.putFile(uri).addOnCompleteListener(task -> {
                                File fdelete = new File(getRealPathFromURI(getContext(), uri));
                                if (fdelete.exists()) {
                                    fdelete.delete();
                                }
                                avatar = null;
                                getStudentsDataFromDatabase(view);
                                //Update
                                updateRecyclerView();
                            });
                        } else {
                            getStudentsDataFromDatabase(view);
                            //Update
                            updateRecyclerView();
                        }
                        //Update in list
                        studentId.remove(oldID);
                        studentId.add(newID);
                        studentList.remove(studentModel);
                        studentList.add(updateStudent);
                        //Cancel dialog
                        editStudentDialog.cancel();
                    }
                });

                //Cancel button
                btnCancel.setOnClickListener(view1 -> editStudentDialog.cancel());
                //open dialog
                editStudentDialog.show();
            }
            if (id == R.id.menu_delete){
                //Initialize delete dialog
                Dialog deleteStudentDialog = new Dialog(getContext());
                deleteStudentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                deleteStudentDialog.setContentView(R.layout.dialog_delete_student);
                deleteStudentDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                deleteStudentDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                EditText etStudentName = deleteStudentDialog.findViewById(R.id.etStudentName);
                EditText etStudentID = deleteStudentDialog.findViewById(R.id.etStudentID);
                Button btnDelete = deleteStudentDialog.findViewById(R.id.btn_delete);
                Button btnCancel = deleteStudentDialog.findViewById(R.id.btn_cancel);

                String name;
                if (studentModel.getLastName() != null){
                    name = studentModel.getLastName() + " " + studentModel.getName();
                } else {
                    name = studentModel.getName();
                }
                etStudentName.setText(name);
                etStudentID.setText(studentModel.getId());

                //Delete button
                btnDelete.setOnClickListener(view1 -> {
                    //Delete the class from database
                    db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId() + "")
                            .collection(STUDENTS_PATH).document(studentModel.getId()).delete();
                    //Remove from the list
                    studentList.remove(studentModel);
                    studentId.remove(studentModel.getId());

                    //Update lesson database
                    db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId()+"")
                            .collection(LESSON_PATH).get().addOnCompleteListener(task -> {
                                if (task.isSuccessful()){
                                    for (QueryDocumentSnapshot lessonSnap : task.getResult()){
                                        LessonModel lessonModel = lessonSnap.toObject(LessonModel.class);
                                        int state = studentModel.getLessonMap().get(lessonModel.getId()+"");
                                        if (state == 1) {
                                            db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId()+"")
                                                    .collection(LESSON_PATH).document(lessonModel.getId() +"").update("present", FieldValue.increment(-1));
                                        }else if (state == 2) {
                                            db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId()+"")
                                                    .collection(LESSON_PATH).document(lessonModel.getId() +"").update("late", FieldValue.increment(-1));
                                        }else if (state == 0){
                                            db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId()+"")
                                                    .collection(LESSON_PATH).document(lessonModel.getId() +"").update("absent", FieldValue.increment(-1));
                                        }
                                    }
                                    //Update lesson fragment
                                    hasChanged = true;
                                    Bundle result = new Bundle();
                                    result.putBoolean("hasChanged", hasChanged);
                                    requireActivity().getSupportFragmentManager().setFragmentResult("result", result);
                                }
                            });

                    StorageReference gsReference = FirebaseStorage.getInstance().getReference(user.getUid() + "/" + SUBJECTS_PATH + "/" +
                            classRoot.getId() + "/" + studentModel.getId());

                    gsReference.listAll().addOnSuccessListener(listResult -> {
                        for(StorageReference student : listResult.getItems()) {
                            student.delete();
                        }
                        for(StorageReference student : listResult.getPrefixes()) {
                            student.listAll().addOnSuccessListener(listResult1 -> {
                               for (StorageReference imageData : listResult1.getItems())
                                   imageData.delete();
                            });
                        }
                    });
                    gsReference.delete();
                    //Refresh recyclerView
                    updateRecyclerView();
                    //No class layout
                    if (studentList.isEmpty()){
                        noStudentLayout.setVisibility(View.VISIBLE);
                    }
                    //Cancel deleteDialog
                    deleteStudentDialog.cancel();
                });

                //Cancel button
                btnCancel.setOnClickListener(view1 -> deleteStudentDialog.cancel());
                //Show deleteDialog
                deleteStudentDialog.show();
            }
            return true;
        });
        //Show popupmenu
        holder.popupMenu.show();
    }

    private void setStudentRecyclerView() {
        //Initialize recyclerView and adapter
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        adapter = new StudentRecyclerViewAdapter(studentList, this, getContext(), user.getUid()+"", classRoot.getId()+"");
        updateRecyclerView();
    }

    private void updateRecyclerView() {
        //Sort
        studentList.sort((studentModel1, t1) -> studentModel1.getName().compareToIgnoreCase(t1.getName()));
        //Update adapter with current list
        adapter.updateList(studentList);
        //Update student number
        tvStudentNumber.setText(String.valueOf(studentList.size()));
        //Set adapter for recyclerView
        recyclerView.setAdapter(adapter);
    }

    private void initWidgets() {
        recyclerView = view.findViewById(R.id.recycler);
        searchView = view.findViewById(R.id.search_student);
        fab = view.findViewById(R.id.fab);
        fabImport = view.findViewById(R.id.fabImport);
        fabAdd = view.findViewById(R.id.fabAdd);

        noStudentLayout = view.findViewById(R.id.noStudent);
        tvStudentNumber = view.findViewById(R.id.tv_student_number);
    }

    //Set searchView
    private void setSearchView(){
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filter(query);
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });
    }

    //Filter function
    private void filter(String values) {
        String text = removeAccent(values).toLowerCase().trim();
        List<StudentModel> list1 = new ArrayList<>();
        for(StudentModel studentModel : studentList) {
            String name = removeAccent(studentModel.getName()).toLowerCase().trim();
            String id = removeAccent(studentModel.getId()).toLowerCase().trim();
            if(name.contains(text) || id.contains(text)){
                list1.add(studentModel);
            }
        }
        adapter.updateList(list1);
        //Sort
        studentList.sort((studentModel1, t1) -> studentModel1.getName().compareToIgnoreCase(t1.getName()));
    }

    public static String removeAccent(String s) {
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("");
    }

    //This function returns true if id is not in studentList
    private boolean isNewId(String id) {
        boolean newId = true;
        for (StudentModel studentModel: studentList){
            if (id.equals(studentModel.getId())){
                newId = false;
                break;
            }
        }
        return newId;
    }
    //This function returns true if updated id is not in studentList and different from oldID
    private boolean checkUpdatedID(String oldID, String newID) {
        boolean isNewID = true;
        for (StudentModel studentModel1 : studentList){
            if (!newID.equals(oldID) && newID.equals(studentModel1.getId())){
                isNewID = false;
                break;
            }
        }
        return isNewID;
    }

    //Show fab
    private void animateFabShow() {
        fabImport.show();
        fabAdd.show();
        isOpen = true;
    }

    //Hide fab
    void animateFabHide(){
        fabImport.hide();
        fabAdd.hide();
        isOpen = false;
    }

    @Override
    public void deleteItem(int position) {
        dataFaces.remove(position);
        faceEmbCreateAdapter.setList(dataFaces);
    }

    @Override
    public void editItem(int position) {

    }

    @Override
    public void getData(String[] listData, String startTime, String endTime, String address, String room) {

    }
}