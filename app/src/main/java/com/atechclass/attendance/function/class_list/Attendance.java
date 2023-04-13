package com.atechclass.attendance.function.class_list;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atechclass.attendance.HideKeyboard;
import com.atechclass.attendance.Language;
import com.atechclass.attendance.R;
import com.atechclass.attendance.adapter.StudentAttendanceAdapter;
import com.atechclass.attendance.interfaces.IColorAttendance;
import com.atechclass.attendance.interfaces.IOnclickStudentATD;
import com.atechclass.attendance.model.ClassModel;
import com.atechclass.attendance.model.LessonModel;
import com.atechclass.attendance.model.StudentModel;
import com.atechclass.attendance.ultis.UserLogin;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class Attendance extends AppCompatActivity implements IOnclickStudentATD, IColorAttendance {
    private static final String SUBJECTS_PATH = "Subjects";
    private static final String STUDENTS_PATH = "Students";
    private static final String USER_PATH = "User";
    private static final String LESSON_PATH = "Lesson";
    private static final int REQUEST_AUTO = 101;
    private static final int REQUEST_PICTURE = 102;

    private Language language;

    private ClassModel classRoot;
    private LessonModel lessonModel;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    private ScrollView noStudentLayout;

    private RecyclerView recyclerView;
    private StudentAttendanceAdapter attendanceAdapter;
    private List<StudentModel> studentList = new ArrayList<>();
    private ArrayList<String> studentId = new ArrayList<>();

    private StorageReference storageReference = FirebaseStorage.getInstance().getReference();

    private Button btnSelectAll;
    private Button btnCancel;
    private Button btnApply;
    private Button btnAdd;
    private Button btnUpdate;
    private RadioButton rdPresent;
    private RadioButton rdAbsent;
    private RadioButton rdLate;
    private ImageButton btnAutomatic;
    private ImageButton btnPicture;
    private ImageButton btnBack;
    private RadioGroup rdgSelectAll;
    private RadioGroup rdgContainerStatus;
    private TextView tvStudentNumber;
    private TextView tvPresent;
    private TextView tvAbsent;
    private TextView tvLate;

    private Spinner spinner;
    private SearchView searchView;
    private int flag;
    private int selectAll;
    private int absent;
    private int present;
    private int late;
    private boolean check = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);
        language = new Language(this);
        language.Language();
        mAuth = UserLogin.getAuth();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();


        ORM();
        setButtons();
        setSearchView();
        getStudentData();
        setStudentRecyclerView();
        selectColor();
        setSpinner();
        addStudent();

        if (user != null) {
            getDataFromDatabase();
        }

        RecognizeAuto(btnAutomatic, new Intent(this, AutoAttendance.class), REQUEST_AUTO);
        RecognizeAuto(btnPicture, new Intent(this, PhotoAttendance.class), REQUEST_PICTURE);
    }

    private void RecognizeAuto(ImageButton btn, Intent intent, int requestCode) {
        btn.setOnClickListener(view -> {
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                if(getFromPerf(this, "ALLOW_KEY")) {
                    //Access setting
                    showSetting();
                } else if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
                }
            } else {
                Bundle bundle = new Bundle();
                bundle.putSerializable("lesson", lessonModel);
                bundle.putSerializable("class", classRoot);
                intent.putExtras(bundle);
                startActivityForResult(intent, requestCode);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_AUTO && resultCode == RESULT_OK) {
            getDataFromDatabase();
            Intent returnIntent = new Intent();
            returnIntent.putExtra("hasChanged", true);
            setResult(RESULT_OK, returnIntent);
        }
        if (requestCode == REQUEST_PICTURE && resultCode == RESULT_OK) {
            getDataFromDatabase();
            Intent returnIntent = new Intent();
            returnIntent.putExtra("hasChanged", true);
            setResult(RESULT_OK, returnIntent);
        }
    }

    private void getStudentData() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        lessonModel = (LessonModel) bundle.getSerializable("lessonModel");
        classRoot = (ClassModel) bundle.getSerializable("class");
    }

    private void getDataFromDatabase(){
        findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId()+"").collection(STUDENTS_PATH)
                .get().addOnCompleteListener(task -> {
                    studentList.clear();
                    studentId.clear();
                    for(QueryDocumentSnapshot studentQuery : task.getResult()){
                        StudentModel studentModel = studentQuery.toObject(StudentModel.class);
                        studentList.add(studentModel);
                        studentId.add(studentModel.getId());
                    }
                    //Update student number
                    tvStudentNumber.setText(String.valueOf(studentList.size()));
                    updateRecyclerView();
                    getPresentAndAbsent();
                    if (task.getResult().isEmpty()) {
                        noStudentLayout.setVisibility(View.VISIBLE);
                    } else {
                        noStudentLayout.setVisibility(View.GONE);
                    }
                    findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                });
    }

    private void getPresentAndAbsent() {
        absent = 0;
        present = 0;
        late = 0;
        for (StudentModel studentModelStatus : studentList){
            int state = studentModelStatus.getLessonMap().getOrDefault(""+ lessonModel.getId(),0);
            switch (state){
                case 0:
                    absent++;
                    break;
                case 1:
                    present++;
                    break;
                case 2:
                default:
                    late++;
            }
        }
        tvAbsent.setText(String.valueOf(absent));
        tvPresent.setText(String.valueOf(present));
        tvLate.setText(String.valueOf(late));
    }
    private void setSpinner() {
        ArrayList<String> students = new ArrayList<>();
        students.add(getString(R.string.all));
        students.add(getString(R.string.txt_present));
        students.add(getString(R.string.txt_absent));
        students.add(getString(R.string.txt_late));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, students);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i){
                    case 0:
                        changelist(-1);
                        break;
                    case 1:
                        changelist(1);
                        break;
                    case 2:
                        changelist(0);
                        break;
                    case 3:
                    default:
                        changelist(2);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Nothing
            }
        });
    }

    private void changelist(int i) {
        ArrayList<StudentModel > list = new ArrayList<>();
        if(i == -1) {
            attendanceAdapter.updateList(studentList);
        }else {
            for (StudentModel studentModelList : studentList){
                int state = studentModelList.getLessonMap().getOrDefault(""+ lessonModel.getId(),0);
                if(state == i) list.add(studentModelList);
            }
            attendanceAdapter.updateList(list);
        }
    }

    // Cập nhật dữ liệu trạng thái sau khi lựa chọn xong tất cả
    private void setButtons(){
        btnUpdate.setOnClickListener(view -> {
            for(StudentModel student : studentList){
                db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId()+"")
                        .collection(STUDENTS_PATH).document(student.getId()).update("lessonMap", student.getLessonMap());
                if(student.getLessonMap().get(lessonModel.getId()+"") != 1) {
                    storageReference.child(user.getUid() + "/" + SUBJECTS_PATH + "/" +
                            classRoot.getId() + "/" + student.getId() + "/attendance/" + lessonModel.getId() + ".jpg").delete();
                }
            }
            db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId()+"")
                    .collection(LESSON_PATH).document(lessonModel.getId()+"")
                    .update("present", present, "absent", absent, "late", late);
            updateRecyclerView();
            getPresentAndAbsent();

            Intent returnIntent = new Intent();
            returnIntent.putExtra("hasChanged", true);
            setResult(RESULT_OK, returnIntent);
        });

        btnBack.setOnClickListener(view -> onBackPressed());

        btnSelectAll.setOnClickListener(view -> changeSelectAll());
    }

    private void setStudentRecyclerView() {
        //Initialize recyclerView and adapter
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        attendanceAdapter = new StudentAttendanceAdapter(studentList, this, lessonModel.getId()+"", this);
        updateRecyclerView();
    }
    private void updateRecyclerView() {
        studentList.sort((studentModel1, t1) -> studentModel1.getName().compareToIgnoreCase(t1.getName()));
        //Update adapter with current list
        attendanceAdapter.updateList(studentList);
        //Set adapter for recyclerView
        recyclerView.setAdapter(attendanceAdapter);
    }
    // 0 là vắng, 1 có mặt, 2 là trễ
    // Hàm dùn để lựa chọn trạng thái từng sinh viên
    public void selectColor(){
        flag = 1;
        attendanceAdapter.updateStatus(flag);
        rdgContainerStatus.setOnCheckedChangeListener((radioGroup, i) -> {
            if (i == R.id.img_check_red){
                flag = 0;
            }
            if (i == R.id.img_check_green){
                flag = 1;
            }
            if (i == R.id.img_check_yellow){
                flag = 2;
            }
            attendanceAdapter.updateStatus(flag);
        });
    }
    void changeSelectAll(){
        // Tạo dialog các lựa chọn
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_select_all_color);
        dialog.show();
        Window window = dialog.getWindow();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        WindowManager.LayoutParams windowAttributes = window.getAttributes();
        windowAttributes.gravity = Gravity.CENTER;
        window.setAttributes(windowAttributes);

        dialog.setCancelable(false);

        rdAbsent = dialog.findViewById(R.id.radio_absent);
        rdLate = dialog.findViewById(R.id.radio_late);
        rdPresent = dialog.findViewById(R.id.radio_present);
        btnCancel = dialog.findViewById(R.id.btn_cancel);
        btnApply = dialog.findViewById(R.id.btn_apply);
        rdgSelectAll = dialog.findViewById(R.id.rdg_select_all_status);

        btnCancel.setOnClickListener(view -> dialog.dismiss());

        // 0 là vắng, 1 có mặt, 2 là trễ
        rdgSelectAll.setOnCheckedChangeListener((radioGroup, i) -> {
            if (i == R.id.radio_present){
                selectAll = 1;
            }
            if (i == R.id.radio_absent){
                selectAll = 0;
            }
            if (i == R.id.radio_late){
                selectAll = 2;
            }
        });
        // Set lựa chọn
        btnApply.setOnClickListener(view -> {
            for (StudentModel studentModel: studentList){
                studentModel.addLesson(String.valueOf(lessonModel.getId()), selectAll);
            }
            updateRecyclerView();
            getPresentAndAbsent();
            dialog.dismiss();
        });
    }

    //Hàm này dùng để click Keyboard
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (getCurrentFocus() != null) {
            HideKeyboard hideKeyboard = new HideKeyboard(this);
            hideKeyboard.closeKeyboard();
        }
        return super.dispatchTouchEvent(ev);
    }

    // Hàm này dùng để search sinh viên bằng id hoặc tên
    void setSearchView(){
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filter(query);
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return false;
            }
        });
    }

    private void filter(String values) {
        ArrayList<StudentModel> list1 = new ArrayList<>();
        for(StudentModel subjects : studentList) {
            if(subjects.getId().toLowerCase().contains(values.toLowerCase()) || subjects.getName().toLowerCase().contains(values.toLowerCase())){
                list1.add(subjects);
            }
        }
        attendanceAdapter.updateList(list1);
        recyclerView.setAdapter(attendanceAdapter);
    }

    private void addStudent(){
        btnAdd.setOnClickListener(view -> {
            Intent intent = new Intent(this, ClassListItem.class);
            intent.putExtra("goToAdd", true);
            setResult(RESULT_CANCELED, intent);
            finish();
        });
    }
    //Hàm ánh xạ view
    private void ORM() {
        spinner = findViewById(R.id.spinner);
        recyclerView = findViewById(R.id.list_attendance);
        btnAutomatic = findViewById(R.id.face);
        btnPicture = findViewById(R.id.camera);
        btnBack = findViewById(R.id.btnBack);
        btnSelectAll = findViewById(R.id.attendance_all);
        rdgContainerStatus = findViewById(R.id.rdg_container_status);
        searchView = findViewById(R.id.search_student_attendance);
        noStudentLayout = findViewById(R.id.noStudent);
        tvStudentNumber = findViewById(R.id.number);
        btnAdd = findViewById(R.id.btn_add_student);
        btnUpdate = findViewById(R.id.btn_update);
        tvPresent = findViewById(R.id.present);
        tvAbsent = findViewById(R.id.absent);
        tvLate = findViewById(R.id.late);
    }

    @Override
    public void updateStatus(Integer listStatus, StudentModel studentModel, StudentAttendanceAdapter.StudentAttendanceViewHolder holder) {
        switch (listStatus){
            case 0:
                holder.check.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.radio_button_checked_red, null));
                break;
            case 1:
                holder.check.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.radio_button_checked_green, null));
                break;
            case 2:
            default:
                holder.check.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.radio_button_checked_yellow, null));
        }
        studentModel.addLesson(String.valueOf(lessonModel.getId()), listStatus);
        getPresentAndAbsent();
    }

    @Override
    public void setColor(int state, StudentAttendanceAdapter.StudentAttendanceViewHolder holder) {
        switch (state){
            case 0:
                holder.check.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.radio_button_checked_red, null));
                break;
            case 1:
                holder.check.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.radio_button_checked_green, null));
                break;
            case 2:
            default:
                holder.check.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.radio_button_checked_yellow, null));
        }
    }

    private void showSetting() {
        new AlertDialog.Builder(Attendance.this)
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
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
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
            checkPermission(grantResults, new Intent(this, AutoAttendance.class), requestCode);
        } else if (requestCode == 102) {
            checkPermission(grantResults, new Intent(this, PhotoAttendance.class), requestCode);
        }
    }

    private void checkPermission(int[] grantResults, Intent intent, int requestCode) {
        int x = 0;
        for(int permission : grantResults) {
            if(permission == PackageManager.PERMISSION_DENIED)
                x++;
        }
        if(x == 0) {
            Bundle bundle = new Bundle();
            bundle.putSerializable("lesson", lessonModel);
            bundle.putSerializable("class", classRoot);
            intent.putExtras(bundle);
            startActivityForResult(intent, requestCode);
        } else {
            boolean showRotinable = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA);
            boolean showRotinable2 = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if(!showRotinable && !showRotinable2) {
                saveToPreferences(this, "ALLOW_KEY", true);
            }
        }
    }
}

