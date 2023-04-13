package com.atechclass.attendance.function.class_list;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.atechclass.attendance.HideKeyboard.closeKeyBoardFragment;
import static com.atechclass.attendance.ultis.CalendarUtils.getToday;
import static com.atechclass.attendance.ultis.CalendarUtils.localDate;
import static com.atechclass.attendance.ultis.SearchUtil.removeAccent;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.AutoTransition;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atechclass.attendance.Language;
import com.atechclass.attendance.adapter.LessonAdapter;
import com.atechclass.attendance.create.LessonCreate;
import com.atechclass.attendance.interfaces.IOnChange;
import com.atechclass.attendance.interfaces.IOnClickItemLessons;
import com.atechclass.attendance.model.ClassModel;
import com.atechclass.attendance.model.LessonModel;
import com.atechclass.attendance.model.StudentModel;
import com.atechclass.attendance.ultis.MyReceiver;
import com.atechclass.attendance.ultis.UserLogin;
import com.atechclass.attendance.R;
import com.atechclass.attendance.databinding.DialogEditLessonBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class Lessons extends Fragment implements IOnClickItemLessons,IOnChange{
    private static final String SUBJECTS_PATH = "Subjects";
    private static final String STUDENTS_PATH = "Students";
    private static final String USER_PATH = "User";
    private static final String LESSON_PATH = "Lesson";
    private static final String HAS_CHANGED = "hasChanged";
    private static final String GO_TO_ADD = "goToAdd";
    private static final String CREATE = "create";
    private static final String ATTENDANCE = "attendance";

    private final List<StudentModel> studentModelList = new ArrayList<>();
    private final ArrayList<String> listIDLesson = new ArrayList<>();

    private RecyclerView listLessons;
    private ArrayList<LessonModel> list;
    private LessonAdapter adapter;
    private Button createLesson;
    private SearchView search;
    private LinearLayout viewLessonEmpty;
    private CoordinatorLayout viewLesson;
    private TextView txtNameClass;
    private TextView txtSumLesson;
    private TextView txtToday;
    private TextView txtSemesterStart;
    private TextView txtSemesterEnd;
    private TextView txtTotalStudent;
    private ProgressBar progressBar;
    private TextView txtRatio;


    private LessonModel newLessonModel = null;

    private int total = 0;
    private FloatingActionButton fabCreateLesson;
    private FloatingActionButton fabMoveToToday;

    private static final int REQUEST_CREATE = 1001;
    private static final int REQUEST_ATTENDANCE = 1002;

    private final Calendar calendar = Calendar.getInstance();
    private final int day = calendar.get(Calendar.DAY_OF_MONTH);
    private final int month = calendar.get(Calendar.MONTH);
    private final int year = calendar.get(Calendar.YEAR);

    private FirebaseFirestore db;
    private FirebaseUser user;
    private View view;

    private ClassModel classModel;

    private final MyReceiver receiver = new MyReceiver();

    private double ratio = 0;
    private Language language;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        language = new Language(getActivity());
        language.Language();
        super.onCreate(savedInstanceState);
        view = inflater.inflate(R.layout.fragment_lessons, container, false);

        //Call back for refreshing this fragment
        requireActivity().getSupportFragmentManager().setFragmentResultListener("students", this, (requestKey, bundle) -> {
            boolean result = bundle.getBoolean(HAS_CHANGED);
            if (result){
                getDB();
            }
        });

        requireActivity().getSupportFragmentManager().setFragmentResultListener("result", this, (requestKey, bundle) -> {
            boolean result = bundle.getBoolean(HAS_CHANGED);
            if (result){
                getDB();
            }
        });

        requireActivity().getSupportFragmentManager().setFragmentResultListener("attendanceFromInfo", this, (requestKey, bundle) -> {
            boolean result = bundle.getBoolean(HAS_CHANGED);
            if (result){
                getDB();
            }
        });

        FirebaseAuth mAuth = UserLogin.getAuth();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();

        if (getArguments() != null) {
            classModel = (ClassModel) getArguments().getSerializable("class");
        }

        closeKeyBoardFragment(getActivity(), view);
        initWidgets(view);
        getDB();
        createLessonValue();
        receiver.initReceverListemner(this);
        return view;
    }

    @Override
    public void isChange(boolean timezone) {
        if(timezone)
            getDB();
    }
    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIME_TICK);
        getActivity().registerReceiver(receiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unregisterReceiver(receiver);
    }

    private void setProgressBar(){
        ArrayList<Double> arrayList = new ArrayList<>();
        int count = 0;

        for(LessonModel lessonModel : list){
            // Kiểm tra xem buổi đó đã có số liệu điểm danh hay chưa
            if(lessonModel.getAbsent() !=0 || lessonModel.getLate() != 0 || lessonModel.getPresent() != 0){
                // tính số sinh viên có mặt trong buổi học đó
                double studentPresent = (lessonModel.getPresent() + lessonModel.getLate()) * 100f / (lessonModel.getPresent() + lessonModel.getLate() + lessonModel.getAbsent());
                // thêm số liệu đã tính được vào một list
                arrayList.add(studentPresent);
                count++;
            }
        }
        for(int i = 0; i < count; i++){
            ratio = ratio + arrayList.get(i);
        }
        if(count == 0) {
            ratio = 0;
            txtRatio.setText("0%");
            progressBar.setProgress((int) ratio);
        } else {
            ratio = ratio/ count;
            progressBar.setProgress((int) ratio);
            DecimalFormat decimalFormat = new DecimalFormat("#.00");
            String ratioText = decimalFormat.format(ratio)+ "%";
            txtRatio.setText(ratioText);
            ratio = 0;
        }
    }
    private void moveToToday(int dayFirst, int dayEnd, LessonAdapter adapter, LinearLayoutManager layoutManager) {
        DateFormat format = new SimpleDateFormat(getActivity().getResources().getString(R.string.fomat_dmy));
        Calendar cldDay = Calendar.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            localDate = LocalDate.now();
        }
        String today = getToday(localDate, getActivity().getString(R.string.fomat_dmy));

        for(int i = 0 ; i < list.size(); i++) {
            cldDay.setTimeInMillis(list.get(i).getDayTime());
            if (today.equals(format.format(list.get(i).getDayTime())) && dayFirst == -1) {
                layoutManager.scrollToPositionWithOffset(i, 0);
                dayFirst = i;
            }
            if (today.equals(format.format(list.get(i).getDayTime()))) {
                dayEnd = i;
            }
        }

        adapter.toDay(dayFirst, dayEnd);

        int finalDayFirst = dayFirst;
        fabMoveToToday.setOnClickListener(view1 -> {
            if(finalDayFirst != -1)
                layoutManager.scrollToPositionWithOffset(finalDayFirst, 0);
            else
                layoutManager.scrollToPositionWithOffset(0, 0);
        });
    }

    private void createLessonValue () {
        fabCreateLesson.setOnClickListener(viewCreate -> createLesson());
    }

    private void getDB () {

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        listLessons.setLayoutManager(layoutManager);
        list = new ArrayList<>();
        view.findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH)
                .document(classModel.getId()+"").collection(LESSON_PATH)
                .get().addOnCompleteListener(task -> {
                    list.clear();
                    if (task.isSuccessful()) {
                        int i = 1;
                        for (QueryDocumentSnapshot document2 : task.getResult()) {
                            LessonModel lessonModel = document2.toObject(LessonModel.class);
                            lessonModel.setPosition(i);
                            list.add(lessonModel);
                            i++;
                        }
                        list.sort(Comparator.comparing(LessonModel::getId).thenComparing(LessonModel::getId));

                        this.view.findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                        setUpList(list, layoutManager);
                    }
                });
        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH)
                .document(classModel.getId()+"").collection(STUDENTS_PATH)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        studentModelList.clear();
                        for (QueryDocumentSnapshot studentSnap: task.getResult()){
                            StudentModel studentModel = studentSnap.toObject(StudentModel.class);
                            studentModelList.add(studentModel);
                        }
                        total = task.getResult().size();
                        txtTotalStudent.setText(String.valueOf(total));
                    }
                });
    }

    int dayFirst = -1;
    int dayEnd = -1;
    private void setUpList(ArrayList<LessonModel> list, LinearLayoutManager layoutManager) {
        if(list.size() != 0) {
            for(LessonModel lessonModel : list) {
                listIDLesson.add(lessonModel.getId() + "");
            }
            localDate = LocalDate.now();
            txtNameClass.setText(classModel.getSubject());
            txtSumLesson.setText(String.valueOf(list.size()));
            txtToday.setText(getToday(localDate, getString(R.string.fomat_dmy)));
            txtSemesterStart.setText(convertFormat(list.get(0).getDayTime(), getString(R.string.fomat_dmy)));
            txtSemesterEnd.setText(convertFormat(list.get(list.size()-1).getDayTime(), getString(R.string.fomat_dmy)));

            viewLesson.setVisibility(View.VISIBLE);
            viewLessonEmpty.setVisibility(View.GONE);
            AutoTransition autoTransition = new AutoTransition();
            autoTransition.excludeChildren(R.id.recycler, true);
            autoTransition.setDuration(100);
            adapter = new LessonAdapter(getActivity(), list, autoTransition, this);
            moveToToday(dayFirst, dayEnd, adapter, layoutManager);
            adapter.filterList(list);
            listLessons.setAdapter(adapter);
            listLessons.setHasFixedSize(true);
            listLessons.setItemViewCacheSize(20);
            setProgressBar();

            search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
        } else {
            viewLesson.setVisibility(View.GONE);
            viewLessonEmpty.setVisibility(View.VISIBLE);
            createLesson.setOnClickListener(viewCreate -> createLesson());
        }
    }

    private String convertFormat(Long dayTime, String typeFormat) {
        DateFormat output = new SimpleDateFormat(typeFormat, Locale.getDefault());
        Calendar calendarConvert = Calendar.getInstance();
        calendarConvert.setTimeInMillis(dayTime);
        return output.format(calendarConvert.getTime());
    }

    private void createLesson() {

        Bundle bundle = new Bundle();
        bundle.putSerializable("class_title", classModel);
        bundle.putSerializable("id_lesson", listIDLesson);

        Intent intentCreate = new Intent(requireActivity().getApplication(), LessonCreate.class);
        intentCreate.putExtras(bundle);
        startActivityForResult(intentCreate, REQUEST_CREATE);
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CREATE && resultCode == RESULT_OK && data != null
                && data.getBooleanExtra(CREATE, true)) {
            getDB();
            Bundle result = new Bundle();
            result.putBoolean(HAS_CHANGED, true);
            //Call back for refreshing main activity & student list
            requireActivity().getSupportFragmentManager().setFragmentResult(CREATE, result);
        }
        if (requestCode == REQUEST_ATTENDANCE && resultCode == RESULT_OK && data != null
                && data.getBooleanExtra(HAS_CHANGED, false)) {
            getDB();
            Bundle result = new Bundle();
            result.putBoolean(HAS_CHANGED, true);
            //Call back for refreshing student list fragment
            requireActivity().getSupportFragmentManager().setFragmentResult(ATTENDANCE, result);
        }
        if (requestCode == REQUEST_ATTENDANCE && resultCode == RESULT_CANCELED && data != null
                && data.getBooleanExtra(GO_TO_ADD, false)){
            Bundle result = new Bundle();
            result.putBoolean(GO_TO_ADD, true);
            //Call back to go to student fragment
            requireActivity().getSupportFragmentManager().setFragmentResult(GO_TO_ADD, result);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void filter(String values) {
        ArrayList<LessonModel> filteredList = new ArrayList<>();
        DateFormat format = new SimpleDateFormat(getString(R.string.fomat_dmy), Locale.getDefault());
        Calendar cldDay = Calendar.getInstance();
        String lesson;
        String room;
        String dayTime;
        String address;
        String valueSearch;
        for(int i = 0; i < list.size() ; i++) {
            //Chuyển đổi millis sang format dd-MM-yyyy
            cldDay.setTimeInMillis(list.get(i).getDayTime());
            //Search buổi
            lesson = (removeAccent(getString(R.string.lesson).toLowerCase() + list.get(i).getPosition()));
            //Search phòng
            room = removeAccent(list.get(i).getRoom());
            //Search ngày
            dayTime = removeAccent(format.format(cldDay.getTime()));
            //Search địa điểm
            address = removeAccent(list.get(i).getAddress());
            //Giá trị search
            valueSearch = removeAccent(values.toLowerCase());

            if(lesson.contains(valueSearch)
                    || dayTime.contains(valueSearch)
                    || room.contains(valueSearch)
                    || address.contains(valueSearch)) {
                filteredList.add(list.get(i));
            }
        }
        adapter.filterList(filteredList);
        listLessons.setAdapter(adapter);
    }

    private void initWidgets(View view) {
        listLessons = view.findViewById(R.id.recycler);
        search = view.findViewById(R.id.search);
        createLesson = view.findViewById(R.id.btn_create_lesson_empty);
        viewLesson = view.findViewById(R.id.view_lesson);
        viewLessonEmpty = view.findViewById(R.id.view_lesson_empty);
        progressBar = view.findViewById(R.id.progressBarLesson);
        txtRatio = view.findViewById(R.id.txt_ratio);

        txtNameClass = view.findViewById(R.id.txt_name_class);
        txtSumLesson = view.findViewById(R.id.txt_sum_lesson);
        txtToday = view.findViewById(R.id.txt_today);
        txtSemesterStart = view.findViewById(R.id.txt_semester_start);
        txtSemesterEnd = view.findViewById(R.id.txt_semester_end);
        txtTotalStudent = view.findViewById(R.id.txt_total_student);
        fabCreateLesson = view.findViewById(R.id.fab_create);
        fabMoveToToday = view.findViewById(R.id.fab_move_top);
    }

    @Override
    public void onClickPopupMenu(LessonAdapter.SubSubjectViewHolder holder, LessonModel lessonModel, String nameLesson) {
        PopupMenu popupMenu = new PopupMenu(requireActivity(), holder.itemView.findViewById(R.id.btn_menu));
        popupMenu.setGravity(Gravity.END);
        popupMenu.getMenuInflater().inflate(R.menu.menu_edit_popup, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_edit){
                DialogEditLessonBinding editBinding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), R.layout.dialog_edit_lesson, null, false);
                Dialog editDialog = new Dialog(getActivity());
                editDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                editDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                editDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                editDialog.setContentView(editBinding.getRoot());

                initDefault(editBinding, lessonModel);

                chooseTime(editBinding);
                chooseDay(editBinding);

                confirmEdit(editBinding, lessonModel, editDialog);
                editDialog.show();
            }
            else {

                Dialog deleteDialog = new Dialog(getContext());
                deleteDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                deleteDialog.setContentView(R.layout.dialog_delete_class);
                deleteDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                deleteDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                EditText etClassName = deleteDialog.findViewById(R.id.et_class_name);
                Button btnDelete = deleteDialog.findViewById(R.id.btn_delete);
                Button btnCancel = deleteDialog.findViewById(R.id.btn_cancel);
                TextView txtConfirm = deleteDialog.findViewById(R.id.txt_confirm_delete);
                txtConfirm.setText(getString(R.string.txt_confirm_delete_lesson));

                DateFormat format = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                Calendar cldDay = Calendar.getInstance();
                cldDay.setTimeInMillis(lessonModel.getDayTime());
                etClassName.setText(nameLesson + " (" + format.format(cldDay.getTime()) + ")");

                //Delete button
                btnDelete.setOnClickListener(view -> {
                    //Delete the class from database
                    db.collection(USER_PATH).document(user.getUid()).collection(getString(R.string.txt_subject))
                            .document(classModel.getId()+"").collection(getString(R.string.txt_lesson)).document(lessonModel.getId()+"").delete();

                    updateStudents(lessonModel);
                    getDB();

                    deleteDialog.cancel();
                });
                //Cancel button
                btnCancel.setOnClickListener(view -> deleteDialog.cancel());
                //Show deleteDialog
                deleteDialog.show();
//                new AlertDialog.Builder(getContext())
//                        .setMessage(R.string.txt_confirm_delete_lesson)
//                                .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
//                                    db.collection(USER_PATH).document(user.getUid()).collection(getString(R.string.txt_subject))
//                                            .document(classModel.getId()+"").collection(getString(R.string.txt_lesson)).document(lessonModel.getId()+"").delete();
//
//                                    updateStudents(lessonModel);
//                                    getDB();
//                                })
//                        .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss()).show();

            }
            return false;
        });

        popupMenu.show();
    }

    @Override
    public void onClick(LessonModel lessonModel) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("lessonModel",lessonModel);
        bundle.putSerializable("class", classModel);
        Intent intent = new Intent(getContext(), Attendance.class);
        intent.putExtras(bundle);
        startActivityForResult(intent, REQUEST_ATTENDANCE);
    }


    private void updateStudents(LessonModel lessonModel) {
        for (StudentModel studentModel: studentModelList){
            //Add new lesson to lessonMap
            studentModel.deleteLesson(String.valueOf(lessonModel.getId()));
            //Upload new document
            db.collection(USER_PATH). document(user.getUid())
                    .collection(SUBJECTS_PATH).document(classModel.getId()+"")
                    .collection(STUDENTS_PATH).document(studentModel.getId()).update("lessonMap", studentModel.getLessonMap());
            FirebaseStorage.getInstance().getReference(user.getUid() + "/" + SUBJECTS_PATH + "/" +
                    classModel.getId() + "/" + studentModel.getId() + "/attendance/" + lessonModel.getId() + ".jpg").delete();
        }
        Bundle result = new Bundle();
        result.putBoolean(HAS_CHANGED, true);
        requireActivity().getSupportFragmentManager().setFragmentResult("delete", result);
    }


    private void confirmEdit(DialogEditLessonBinding editBinding, LessonModel lessonModel, Dialog editDialog) {
        editBinding.dialogBtnConfirm.setOnClickListener(view1 -> {
            if(editBinding.dialogEdtAddress.length() != 0 && editBinding.dialogEdtClassRoom.length() != 0) {
                if(checkTime(editBinding.dialogTimeStart, editBinding.dialogTimeEnd, getString(R.string.fomat_hm), editBinding.imgError)) {
                    db.collection(USER_PATH).document(user.getUid())
                            .collection(SUBJECTS_PATH).document(classModel.getId()+"")
                            .collection(LESSON_PATH).document(lessonModel.getId()+"").delete();
                    try {
                        SimpleDateFormat formatId = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                        DateFormat formatTime = new SimpleDateFormat("HH:mm", Locale.getDefault());

                        Calendar editDateCalendar = Calendar.getInstance();
                        editDateCalendar.setTime(Objects.requireNonNull(formatId.parse(editBinding.dialogTxtDayStart.getText().toString())));

                        Calendar editTimeStartCalendar = Calendar.getInstance();
                        editTimeStartCalendar.setTime(Objects.requireNonNull(formatTime.parse(editBinding.dialogTimeStart.getText().toString())));

                        Calendar editTimeEndCalendar = Calendar.getInstance();
                        editTimeEndCalendar.setTime(Objects.requireNonNull(formatTime.parse(editBinding.dialogTimeEnd.getText().toString())));

                        newLessonModel = new LessonModel(editDateCalendar.getTimeInMillis(), editTimeStartCalendar.getTimeInMillis()
                                , editTimeEndCalendar.getTimeInMillis(), editBinding.dialogEdtAddress.getText().toString()
                                , editBinding.dialogEdtClassRoom.getText().toString(), lessonModel.getFlag());

                        newLessonModel.setAbsent(lessonModel.getAbsent());
                        newLessonModel.setPresent(lessonModel.getPresent());
                        newLessonModel.setLate(lessonModel.getLate());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    if(lessonModel.getId() != newLessonModel.getId()) {
                        StorageReference reference = FirebaseStorage.getInstance().getReference();
                        StorageReference newReference = FirebaseStorage.getInstance().getReference();
                        for(StudentModel studentModel : studentModelList) {
                            reference.child(user.getUid() + "/" + SUBJECTS_PATH + "/" + classModel.getId() +
                                    "/" + studentModel.getId() + "/attendance/" + lessonModel.getId() +".jpg").getBytes(500000)
                                    .addOnCompleteListener(task -> {
                                        newReference.child(user.getUid() + "/" + SUBJECTS_PATH + "/" + classModel.getId() +
                                                "/" + studentModel.getId() + "/attendance/" + newLessonModel.getId() +".jpg").putBytes(task.getResult());
                                        FirebaseStorage.getInstance().getReference(user.getUid() + "/" + SUBJECTS_PATH + "/" +
                                                classModel.getId() + "/" + studentModel.getId() + "/attendance/" + lessonModel.getId() + ".jpg").delete();
                                    });
                            int state = studentModel.getLessonMap().get(String.valueOf(lessonModel.getId()));
                            studentModel.deleteLesson(String.valueOf(lessonModel.getId()));
                            studentModel.addLesson(String.valueOf(newLessonModel.getId()), state);
                            //Upload new document
                            db.collection(USER_PATH). document(user.getUid())
                                    .collection(SUBJECTS_PATH).document(classModel.getId()+"")
                                    .collection(STUDENTS_PATH).document(studentModel.getId()).update("lessonMap", studentModel.getLessonMap());

                        }
                    }
                    db.collection(USER_PATH).document(user.getUid())
                            .collection(SUBJECTS_PATH).document(classModel.getId()+"")
                            .collection(LESSON_PATH).document(newLessonModel.getId()+"")
                            .set(newLessonModel);
                    getDB();
                    editDialog.cancel();
                } else {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
                    dialog.setTitle(getString(R.string.invalid_time))
                            .setNegativeButton("OK", (dialogInterface, i) -> dialogInterface.dismiss()).show();
                }
            } else {
                if(editBinding.dialogEdtAddress.getText().length() == 0)
                    editBinding.dialogEdtAddress.setError(getString(R.string.address_not_empty));
                if(editBinding.dialogEdtClassRoom.getText().length() == 0)
                    editBinding.dialogEdtClassRoom.setError(getString(R.string.classRoom_not_empty));
            }
        });
    }

    private void initDefault(DialogEditLessonBinding editBinding, LessonModel lessonModel) {
        DateFormat formatTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Date dateStart = new Date(lessonModel.getStartTime());
        Date dateEnd = new Date(lessonModel.getEndTime());
        editBinding.dialogEdtClassRoom.setText(lessonModel.getRoom());
        editBinding.dialogEdtAddress.setText(lessonModel.getAddress());
        editBinding.dialogTimeStart.setText(formatTime.format(dateStart));
        editBinding.dialogTimeEnd.setText(formatTime.format(dateEnd));
        DateFormat format = new SimpleDateFormat(getString(R.string.fomat_dmy), Locale.getDefault());
        Calendar cldDay = Calendar.getInstance();
        cldDay.setTimeInMillis(lessonModel.getDayTime());
        editBinding.dialogTxtDayStart.setText(format.format(cldDay.getTime()));
    }

    private void chooseDay(DialogEditLessonBinding editBinding) {
        editBinding.dialogContainerDayStart.setOnClickListener(view1 -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), R.style.MyDatePickerStyle, (datePicker, i, i1, i2) -> {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(getString(R.string.fomat_dmy), Locale.getDefault());
                calendar.set(i, i1, i2);
                editBinding.dialogTxtDayStart.setText(simpleDateFormat.format(calendar.getTime()));
            }, year, month, day);
            datePickerDialog.show();
        });
    }

    //Chọn thời gian định dạng HH:mm
    private void chooseTime(DialogEditLessonBinding editBinding) {
        handleClickTime(editBinding.dialogTimeStart, editBinding.dialogContainerTimeStart);
        handleClickTime(editBinding.dialogTimeEnd, editBinding.dialogContainerTimeEnd);
        textWatcher(editBinding.dialogTimeStart, editBinding.dialogTimeEnd, getString(R.string.fomat_hm), editBinding.imgError);
    }

    //Chọn thời gian và hiển thị trên display
    private void handleClickTime(TextView time, LinearLayout containerTime) {
        Calendar clickedCalendar = Calendar.getInstance();
        int hour = clickedCalendar.get(Calendar.HOUR_OF_DAY);
        int minute = clickedCalendar.get(Calendar.MINUTE);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(getString(R.string.fomat_hm), Locale.getDefault());
        containerTime.setOnClickListener(view -> {
            TimePickerDialog pickerDialog = new TimePickerDialog(view.getContext(), R.style.MyDatePickerStyle, (timePicker, i, i1) -> {
                clickedCalendar.set(0, 0, 0, i, i1);
                time.setText(simpleDateFormat.format(clickedCalendar.getTime()));
            }, hour, minute, true);
            pickerDialog.show();
        });
    }

    //Kiểm tra sự thay đổi của text
    private void textWatcher(TextView timeStart, TextView timeEnd, String format, ImageView imgError) {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                checkTime(timeStart, timeEnd, format, imgError);
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //Do nothing
            }

            @Override
            public void afterTextChanged(Editable editable) {
                checkTime(timeStart, timeEnd, format, imgError);
            }
        };
        timeStart.addTextChangedListener(watcher);
        timeEnd.addTextChangedListener(watcher);
    }

    //Kiểm tra thời gian có hợp lệ không
    private boolean checkTime(TextView timeStart, TextView timeEnd, String format, ImageView imgError) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        try {
            Date dayStartCheck = sdf.parse(timeStart.getText().toString());
            Date dayEndCheck = sdf.parse(timeEnd.getText().toString());
            //Kiểm tra ngày bắt đầu có lớn hơn ngày kết thúc không
            if (dayEndCheck != null && dayStartCheck != null) {
                if(dayStartCheck.getTime() > dayEndCheck.getTime()) {
                    imgError.setVisibility(View.VISIBLE);
                    return false;
                } else {
                    imgError.setVisibility(View.GONE);
                    return true;
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }
}