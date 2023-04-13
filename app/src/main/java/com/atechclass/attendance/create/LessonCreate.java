package com.atechclass.attendance.create;

import static com.atechclass.attendance.ultis.CalendarUtils.getDayRepeatMonth;
import static com.atechclass.attendance.ultis.CalendarUtils.getEndOfWeek;
import static com.atechclass.attendance.ultis.CalendarUtils.getWeekNow;
import static com.atechclass.attendance.ultis.CalendarUtils.localDate;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;

import com.atechclass.attendance.HideKeyboard;
import com.atechclass.attendance.Language;
import com.atechclass.attendance.R;
import com.atechclass.attendance.adapter.AddressAdapter;
import com.atechclass.attendance.adapter.RoomAdapter;
import com.atechclass.attendance.databinding.ActivityLessonCreateBinding;
import com.atechclass.attendance.home.SpinnerDropdown;
import com.atechclass.attendance.model.ClassModel;
import com.atechclass.attendance.model.LessonModel;
import com.atechclass.attendance.model.SniperHome;
import com.atechclass.attendance.model.StudentModel;
import com.atechclass.attendance.ultis.UserLogin;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LessonCreate extends AppCompatActivity {

    private  String [] spinnerList;
    private static final String SUBJECTS_PATH = "Subjects";
    private static final String STUDENTS_PATH = "Students";
    private static final String USER_PATH = "User";
    private static final String LESSON_PATH = "Lesson";
    private static final String HOUR_FORMAT = "HH:mm";
    private static final String DATE_OUTPUT_FORMAT = "EEE, dd MMM, yyyy";
    private static final String DATE_FORMAT = "dd/MM/yyyy";
    private final DateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyy", Locale.getDefault());
    Language language;
    ActivityLessonCreateBinding binding;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser user;


    Spinner spinnerRepeat;
    int typeSpinnerRepeat;
    int typeSpinnerMonth;
    boolean flagRdb = false;
    String dayInt;
    String dayS;

    SpinnerDropdown dropdownMonth;
    View viewDropDownMonthRepeat;

    final Calendar calendar = Calendar.getInstance();
    int day = calendar.get(Calendar.DAY_OF_MONTH);
    int month = calendar.get(Calendar.MONTH);
    int year = calendar.get(Calendar.YEAR);

    private ClassModel classModel;
    private final List<StudentModel> studentModelList = new ArrayList<>();
    private final List<LessonModel> list = new ArrayList<>();
    private ArrayList<String> listIDLesson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_create);
        language = new Language(this);
        language.Language();
        mAuth = UserLogin.getAuth();
        db = FirebaseFirestore.getInstance();

        spinnerList = new String[] {getString(R.string.every_day), getString(R.string.every_week),getString( R.string.every_month)};

        binding = DataBindingUtil.setContentView(this, R.layout.activity_lesson_create);
        Toolbar toolbar = binding.toolbarCreateLesson;
        toolbar.setNavigationIcon(R.drawable.ic_back);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        repeat();
        timeRepeat();
        dropDownRepeat();
        chooseTime();
        setDefautDayOfWeek();
        checkEdtInput();
        confirmCreate();
    }

    private void autoCompleteTextView() {
        AddressAdapter addressAdapter = new AddressAdapter(this, R.layout.layout_custom_suggest, getListAddress());
        binding.edtAddress.setAdapter(addressAdapter);

        RoomAdapter roomAdapter = new RoomAdapter(this, R.layout.layout_custom_suggest, getListRoom());
        binding.edtClassRoom.setAdapter(roomAdapter);
    }

    private List<String> getListRoom() {
        List<String> listRoom = new ArrayList<>();
        for(LessonModel lessonModel : list){
            if (!listRoom.contains(lessonModel.getRoom())){
                listRoom.add(lessonModel.getRoom());
            }
        }
        return listRoom;
    }

    private List<String> getListAddress() {
        List<String> listAddress = new ArrayList<>();
        for(LessonModel lessonModel : list){
            if (!listAddress.contains(lessonModel.getAddress())){
                listAddress.add(lessonModel.getAddress());
            }
        }
        return listAddress;
    }

    @Override
    protected void onStart() {
        super.onStart();
        user = mAuth.getCurrentUser();
        if (user != null) {
            getData();
            getDataFromDatabase();
        }

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (getCurrentFocus() != null) {
            HideKeyboard hideKeyboard = new HideKeyboard(this);
            hideKeyboard.closeKeyboard();
        }
        return super.dispatchTouchEvent(ev);
    }

    private void getData() {
        Intent bundle = getIntent();
        classModel = (ClassModel) bundle.getSerializableExtra("class_title");
        listIDLesson = bundle.getStringArrayListExtra("id_lesson");
    }

    private void getDataFromDatabase() {
        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH)
                .document(classModel.getId()+"").collection(STUDENTS_PATH)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        studentModelList.clear();
                        for (QueryDocumentSnapshot studentSnap: task.getResult()){
                            StudentModel studentModel = studentSnap.toObject(StudentModel.class);
                            studentModelList.add(studentModel);
                        }
                    }
                });

        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classModel.getId() + "").collection(LESSON_PATH)
                .get().addOnCompleteListener(task -> {
                    list.clear();
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            LessonModel lessonModel = document.toObject(LessonModel.class);
                            list.add(lessonModel);
                        }
                        autoCompleteTextView();
                    }
                });
    }

    //Kiểm tra input phòng học và địa điểm là trống
    private void checkEdtInput() {
        binding.txtDayStart.setText(simpleDateFormat.format(calendar.getTime()));
        edtCheck(binding.edtClassRoom, getString(R.string.classRoom_not_empty));
        edtCheck(binding.edtAddress, getString(R.string.address_not_empty));
    }

    //Hàm set default cho checkbox trong lặp lại tuần
    private void setDefautDayOfWeek() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            localDate = LocalDate.now();
        }
        List<String> listDay = getWeekNow(localDate, "EEE");
        binding.cbMon.setText(listDay.get(0));
        binding.cbTue.setText(listDay.get(1));
        binding.cbWed.setText(listDay.get(2));
        binding.cbThu.setText(listDay.get(3));
        binding.cbFri.setText(listDay.get(4));
        binding.cbSat.setText(listDay.get(5));
        binding.cbSun.setText(listDay.get(6));
    }

    private void edtCheck(AppCompatAutoCompleteTextView edtCheck, String notify) {
        //Sử dụng textchange để bắt sự thay đổi của ô edit text
        //Nếu chiều dài của đoạn text là 0 thì sẽ thông báo lỗi empty
        edtCheck.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //Do nothing
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence.length() <= 0)
                    edtCheck.setError(notify);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                //Do nothing
            }
        });
    }

    //Hàm kiểm tra và xác nhận khi khởi tạo dữ liệu
    private void confirmCreate() {
        binding.btnConfirm.setOnClickListener(view -> {
            //Khởi tạo danh sách lưu các ngày
            List<Long> listDay = new ArrayList<>();
            //Kiểm tra phòng họp và địa điểm có phải rỗng
            if(binding.edtClassRoom.getText().length() != 0 && binding.edtAddress.getText().length() != 0) {
                //Kiểm tra thời gian và ngày có hợp lệ hay không
                if(checkTime(binding.timeStart, binding.timeEnd, HOUR_FORMAT, binding.imgError, true)
                && checkTime(binding.txtDayStart, binding.txtDayEnd, DATE_FORMAT, binding.imgErrorDay, flagRdb)) {
                    //Kiểm tra có sử dụng tính năng lặp lại hay không
                    if(binding.swRepeat.isChecked()) {
                        //Kiểu lặp lại
                        switch (typeSpinnerRepeat) {
                            case 0:
                                //Kiểm tra lặp lại đến ngày kết thúc
                                if(binding.rdbRepateTo.isChecked()) {
                                    listDay = calculatorRepeat(binding.txtDayStart, binding.txtDayEnd);
                                }
                                //Kiểm tra lặp lại x lần
                                else if(binding.rdbRepeatTime.isChecked() &&
                                        binding.edtRepateTime.getText().toString().length() != 0) {
                                    listDay = calculatorTime(binding.txtDayStart, binding.edtRepateTime);
                                }
                                break;
                            case 1:getDayOfWeek();
                                ArrayList<String> dayOfWeek = getDayOfWeek();
                                //Kiểm tra lặp lại đến ngày kết thúc
                                if(binding.rdbRepateTo.isChecked()) {
                                    listDay = calculatorRepeatWeek(binding.txtDayStart, binding.txtDayEnd, dayOfWeek);
                                }
                                //Kiểm tra lặp lại x lần
                                else if(binding.rdbRepeatTime.isChecked() &&
                                        binding.edtRepateTime.getText().toString().length() != 0) {
                                    listDay = calculatorTimeWeek(binding.txtDayStart, binding.edtRepateTime, dayOfWeek);
                                }
                                break;
                            case 2:
                                //Kiểm tra lặp lại đến ngày kết thúc
                                if(typeSpinnerMonth == 0) {
                                    //Kiểm tra tính năng tháng lặp chọn ngày hàng tháng
                                    if(binding.rdbRepateTo.isChecked()) {
                                        listDay = calculatorRepeatToMonth(binding.txtDayStart, binding.txtDayEnd);
                                    }
                                    // Kiểm tra tính năng tháng lặp chọn lặp vào thứ đầu tiên của tháng
                                    else if(binding.rdbRepeatTime.isChecked() &&
                                            binding.edtRepateTime.getText().toString().length() != 0) {
                                        listDay = calculatorTimeNTTime(binding.txtDayStart, binding.edtRepateTime);
                                    }
                                }
                                //Kiểm tra lặp lại x lần
                                else if (typeSpinnerMonth == 1) {
                                    //Kiểm tra tính năng tháng lặp chọn ngày hàng tháng
                                    if(binding.rdbRepateTo.isChecked()) {
                                        listDay = calculatorRepeatMonth(binding.txtDayStart, binding.txtDayEnd);
                                    }
                                    // Kiểm tra tính năng tháng lặp chọn lặp vào thứ đầu tiên của tháng
                                    else if(binding.rdbRepeatTime.isChecked() &&
                                            binding.edtRepateTime.getText().toString().length() != 0) {
                                        listDay = calculatorTimeMonth(binding.txtDayStart, binding.edtRepateTime);
                                    }
                                }
                                break;
                        }
                    } else {
                        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
                        Date date = null;
                        try {
                            date = dateFormat.parse(binding.txtDayStart.getText().toString());
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        Calendar confirmCalendar = Calendar.getInstance();
                        confirmCalendar.setTime(date);
                        listDay.add(confirmCalendar.getTimeInMillis());
                    }

                    //Đưa dữ liệu lên firestore firebase
                    try {
                        constructorLesson(listDay);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                } else {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                    dialog.setTitle(R.string.invalid_time)
                            .setNegativeButton("OK", (dialogInterface, i) -> dialogInterface.dismiss()).show();
                }
            } else {
                if(binding.edtClassRoom.getText().length() == 0) {
                    binding.edtClassRoom.setError(getString(R.string.classRoom_not_empty));
                    binding.edtClassRoom.requestFocus();
                } else if(binding.edtAddress.getText().length() == 0) {
                    binding.edtAddress.setError(getString(R.string.address_not_empty));
                    binding.edtAddress.requestFocus();
                }
            }
        });
    }
    private boolean isDuplicate = false;
    private void constructorLesson(List<Long> listDay) throws ParseException {
        //Khởi tạo danh sách với đối tượng được truyền vào là Lesson
        ArrayList<LessonModel> lessonModels = new ArrayList<>();
        //Lặp các ngày để đưa vào danh sách đối tượng
        for(Long dayConstruct : listDay) {
            try {
                lessonModels.add(new LessonModel(dayConstruct, startTime.getTimeInMillis(), endTime.getTimeInMillis(),
                        binding.edtAddress.getText().toString(), binding.edtClassRoom.getText().toString(), 1));

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        for(LessonModel lessonModel : lessonModels) {
            for(String dayDuplicate : listIDLesson) {
                if(dayDuplicate.equals(lessonModel.getId()+"")) {
                    isDuplicate = true;
                    break;
                }
            }
        }

        if(isDuplicate) {
            new AlertDialog.Builder(this)
                    .setTitle("Thời gian bị trùng lặp, bạn có muốn ghi đè không?")
                    .setPositiveButton("OK", (dialogInterface, i) -> {
                        //Check if lessons list is empty
                        pushDataToFirebase(lessonModels);
                        dialogInterface.dismiss();
                    }).setNegativeButton("Hủy", (dialogInterface, i) -> dialogInterface.dismiss()).show();
        } else {
            //Check if lessons list is empty
            pushDataToFirebase(lessonModels);
        }
    }

    private void pushDataToFirebase(ArrayList<LessonModel> lessonModels) {
        if (lessonModels.size() == 0) {
            //Dialog empty
            new AlertDialog.Builder(LessonCreate.this)
                    .setTitle(R.string.no_lessons_create)
                    .setMessage(getString(R.string.do_you_want_continue))
                    .setNegativeButton(getString(R.string.noneCol), (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                        onBackPressed();
                        finish();
                    }).setPositiveButton(getString(R.string.yes), (dialogInterface, i) -> dialogInterface.dismiss()).show();
        } else {
            //Upload data to db
            for (LessonModel lessonModel : lessonModels) {
                //Upload lesson
                db.collection(USER_PATH).document(user.getUid())
                        .collection(SUBJECTS_PATH).document(classModel.getId() + "")
                        .collection(LESSON_PATH).document(lessonModel.getId() + "")
                        .set(lessonModel);
                //Update student
                updateStudents(lessonModel);
            }
            Intent notice = new Intent();
            notice.putExtra("create", true);
            setResult(RESULT_OK, notice);
            finish();
        }
    }

    @NonNull
    private Calendar convertCalendar(SimpleDateFormat format, TextView p) throws ParseException {
        Date timeEnd = format.parse(p.getText().toString());
        Calendar calendarTimeEnd = Calendar.getInstance();
        calendarTimeEnd.setTime(timeEnd);
        return calendarTimeEnd;
    }

    private void updateStudents(LessonModel lessonModel) {
        for (StudentModel studentModel: studentModelList){
            //Add new lesson to lessonMap
            studentModel.addLesson(String.valueOf(lessonModel.getId()), 0);
            //Upload new document
            db.collection(USER_PATH). document(user.getUid())
                    .collection(SUBJECTS_PATH).document(classModel.getId()+"")
                    .collection(STUDENTS_PATH).document(studentModel.getId()).update("lessonMap", studentModel.getLessonMap());
        }
    }

    //Calculate days repeat n times on day x every months
    private List<Long> calculatorTimeNTTime(TextView txtDayStart, EditText repetition) {
        ArrayList<Long> calTimeNNTimeList = new ArrayList<>();
        Date date = null;
        try {
            date = simpleDateFormat.parse(txtDayStart.getText().toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar calendarBefore = Calendar.getInstance();
        calendarBefore.setTime(date);
        int calTimeNNTimeDay = calendarBefore.getTime().getDate();
        //Số lần cần lặp của ngày x của hàng tháng
        for(int i = 0; i < Integer.parseInt(repetition.getText().toString()); i++) {
            //Nếu ngày bằng tháng sau thì thêm ngày vào danh sách
            if(calTimeNNTimeDay == calendarBefore.getTime().getDate()) {
                calTimeNNTimeList.add(calendarBefore.getTimeInMillis());
                calendarBefore.add(Calendar.MONTH, 1);
            } else
            //Tăng lên một tháng và giảm i đi 1 để lặp lại
            {
                calendarBefore.add(Calendar.MONTH, 1);
                calendarBefore.set(Calendar.DAY_OF_MONTH, calTimeNNTimeDay);
                i--;
            }
        }
        return calTimeNNTimeList;
    }

    //Tính danh sách các buổi lặp x lần với feature đến ngày kết thúc
    private List<Long> calculatorRepeatToMonth(TextView txtDayStart, TextView txtDayEnd) {
        ArrayList<Long> repeatMonthList = new ArrayList<>();
        Date date1 = null;
        Date date2 = null;
        try {
            date1 = simpleDateFormat.parse(txtDayStart.getText().toString());
            date2 = simpleDateFormat.parse(txtDayEnd.getText().toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar cldDayStart = Calendar.getInstance();
        cldDayStart.setTime(date1);
        Calendar cldDayEnd = Calendar.getInstance();
        cldDayEnd.setTime(date2);
        int repeatMonthDay = cldDayStart.getTime().getDate();
        //Lặp kiểm tra ngày bắt đầu trước ngày kết thúc
        while (!cldDayStart.after(cldDayEnd)) {
            //Nếu ngày bằng tháng sau thì thêm ngày vào danh sách
            if(repeatMonthDay == cldDayStart.getTime().getDate()) {
                repeatMonthList.add(cldDayStart.getTimeInMillis());
                cldDayStart.add(Calendar.MONTH, 1);
            } else
            //Tăng lên từng ngày để lặp
            {
                cldDayStart.add(Calendar.DATE, 1);
            }
        }
        return repeatMonthList;
    }

    //Tính danh sách lặp n lần của thứ đầu tiên của tháng
    private List<Long> calculatorTimeMonth(TextView txtDayStart, EditText repetition) {
        List<Long> calTimeMonthList = null;
        Date date = null;
        try {
            date = simpleDateFormat.parse(txtDayStart.getText().toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar calTimeMonthCalendar = Calendar.getInstance();
        calTimeMonthCalendar.setTime(date);
        Date input = calTimeMonthCalendar.getTime();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate timeDay = input.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            calTimeMonthList = getDayRepeatMonth(timeDay, repetition);
        }
        return calTimeMonthList;
    }

    //Tính danh sách lặp đến thứ đầu tiên tới ngày kết thúc
    private List<Long> calculatorRepeatMonth(TextView txtDayStart, TextView txtDayEnd) {
        ArrayList<Long> calRepeatMonthList = new ArrayList<>();
        DateFormat output = new SimpleDateFormat(DATE_OUTPUT_FORMAT, Locale.getDefault());
        DateFormat dateFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        Date date1 = null;
        Date date2 = null;
        try {
            date1 = simpleDateFormat.parse(txtDayStart.getText().toString());
            date2 = simpleDateFormat.parse(txtDayEnd.getText().toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar dayStart = Calendar.getInstance();
        dayStart.setTime(date1);
        Calendar dayEnd = Calendar.getInstance();
        dayEnd.setTime(date2);
        //Mặt định thứ x
        String dayRepeat = dateFormat.format(dayStart.getTime());
        //Lặp ngày bắt đầu phía trước ngày kết thúc
        while (!dayStart.after(dayEnd)) {
            String calRepeatMonthDay = output.format(dayStart.getTime());
            //Kiểm tra thứ x có bằng thứ đầu tiên của tháng sau không
            //Nếu bằng thì sẽ thêm vào danh sách và tháng tiếp theo tăng lên 1 và set về ngày dầu tiên của tháng
            if(calRepeatMonthDay.contains(dayRepeat)) {
                calRepeatMonthList.add(dayStart.getTimeInMillis());
                dayStart.add(Calendar.MONTH, 1);
                dayStart.set(Calendar.DAY_OF_MONTH, 1);
            } else {
                dayStart.add(Calendar.DATE, 1);
            }
        }
        return calRepeatMonthList;
    }

    //Lấy danh sách các ngày của tuần
    private ArrayList<String> getDayOfWeek() {
        ArrayList<String> dayOfWeek = new ArrayList<>();
        if(binding.cbMon.isChecked()) {
            dayOfWeek.add(binding.cbMon.getText().toString());
        }
        if(binding.cbTue.isChecked()) {
            dayOfWeek.add(binding.cbTue.getText().toString());
        }
        if(binding.cbWed.isChecked()) {
            dayOfWeek.add(binding.cbWed.getText().toString());
        }
        if(binding.cbThu.isChecked()) {
            dayOfWeek.add(binding.cbThu.getText().toString());
        }
        if(binding.cbFri.isChecked()) {
            dayOfWeek.add(binding.cbFri.getText().toString());
        }
        if(binding.cbSat.isChecked()) {
            dayOfWeek.add(binding.cbSat.getText().toString());
        }
        if(binding.cbSun.isChecked()) {
            dayOfWeek.add(binding.cbSun.getText().toString());
        }
        return dayOfWeek;
    }

    //Tính danh sách các tuần lặp n lần
    private List<Long> calculatorTimeWeek(TextView txtDayStart, EditText repetition, ArrayList<String> dayOfWeek) {
        ArrayList<Long> calTimeWeekList = new ArrayList<>();
        DateFormat formatConvert = new SimpleDateFormat(DATE_OUTPUT_FORMAT, Locale.getDefault());
        Date date = null;
        try {
            date = simpleDateFormat.parse(txtDayStart.getText().toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar dayStart = Calendar.getInstance();
        Date input = dayStart.getTime();
        dayStart.setTime(date);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate lcdDay = input.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            List<String> listDayOfWeek = getEndOfWeek(lcdDay, DATE_OUTPUT_FORMAT, repetition);
            //Lặp danh sách hàng tuần
            for(String calTimeWeekDay : listDayOfWeek) {
                //Lặp qua danh sách các ngày
                for(String dayRepeat : dayOfWeek) {
                    //Kiểm tra nếu thứ x bằng thứ y thì thêm ngày vào danh sách
                    if (calTimeWeekDay.contains(dayRepeat)) {
                        try {
                            date = formatConvert.parse(calTimeWeekDay);
                            dayStart.setTime(date);
                            calTimeWeekList.add(dayStart.getTimeInMillis());
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return calTimeWeekList;
    }

    //Repeat week until dayEnd
    private List<Long> calculatorRepeatWeek(TextView txtDayStart, TextView txtDayEnd, ArrayList<String> dayOfWeek) {
        ArrayList<Long> calRepeatWeekList = new ArrayList<>();
        DateFormat output = new SimpleDateFormat(DATE_OUTPUT_FORMAT, Locale.getDefault());
        Date date1 = null;
        Date date2 = null;
        try {
            date1 = simpleDateFormat.parse(txtDayStart.getText().toString());
            date2 = simpleDateFormat.parse(txtDayEnd.getText().toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar dayStart = Calendar.getInstance();
        dayStart.setTime(date1);
        Calendar dayEnd = Calendar.getInstance();
        dayEnd.setTime(date2);
        //Repeat if dayStart is before dayEnd
        while (!dayStart.after(dayEnd)) {
            String calRepeatWeekDay = output.format(dayStart.getTime());
            //Repeat days in a week
            for(String dayRepeat : dayOfWeek) {
                //Check if day == dayRepeat, add to the list
                if(calRepeatWeekDay.contains(dayRepeat)) {
                    calRepeatWeekList.add(dayStart.getTimeInMillis());
                }
            }
            dayStart.add(Calendar.DAY_OF_WEEK, 1);
        }
        return calRepeatWeekList;
    }

    //Calculate repeat list n times of chosen date
    private List<Long> calculatorTime(TextView txtDayStart, EditText repetition) {
        ArrayList<Long> calTimeList = new ArrayList<>();
        Date date = null;
        try {
            date = simpleDateFormat.parse(txtDayStart.getText().toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar calTimeCalendar = Calendar.getInstance();
        calTimeCalendar.setTime(date);
        //Repeat times
        for(int i = 0; i < Integer.parseInt(repetition.getText().toString()); i++) {
            calTimeList.add(calTimeCalendar.getTimeInMillis());
            calTimeCalendar.add(Calendar.DATE, 1);
        }
        return calTimeList;
    }

    //Calculate repeat list until endDay
    private List<Long> calculatorRepeat(TextView txtDayStart, TextView txtDayEnd) {
        ArrayList<Long> calRepeatList = new ArrayList<>();
        Date date1 = null;
        Date date2 = null;
        try {
            date1 = simpleDateFormat.parse(txtDayStart.getText().toString());
            date2 = simpleDateFormat.parse(txtDayEnd.getText().toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar startDay = Calendar.getInstance();
        startDay.setTime(date1);
        Calendar endDay = Calendar.getInstance();
        endDay.setTime(date2);

        //Repeat if startDay is before endDay
        while (!startDay.after(endDay)) {
            calRepeatList.add(startDay.getTimeInMillis());
            startDay.add(Calendar.DATE, 1);
        }
        return calRepeatList;
    }

    Calendar startTime = Calendar.getInstance();
    Calendar endTime = Calendar.getInstance();
    //Choose time format HH:mm
    private void chooseTime() {
        handleClickTime(binding.timeStart, binding.containerTimeStart, startTime, false);
        handleClickTime(binding.timeEnd, binding.containerTimeEnd, endTime, true);
        textWatcher(binding.timeStart, binding.timeEnd, HOUR_FORMAT, binding.imgError, true);
    }

    //TextWatcher
    private void textWatcher(TextView timeStart, TextView timeEnd, String format, ImageView imgError, boolean b) {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                checkTime(timeStart, timeEnd, format, imgError, b);
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //Do nothing
            }

            @Override
            public void afterTextChanged(Editable editable) {
                DateFormat output = new SimpleDateFormat("EEEE", Locale.getDefault());
                Date date = null;
                try {
                    date = simpleDateFormat.parse(binding.txtDayStart.getText().toString());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                Calendar textWatcherCalendar = Calendar.getInstance();
                textWatcherCalendar.setTime(date);
                dayInt = textWatcherCalendar.getTime().getDate() + "";
                dayS = output.format(textWatcherCalendar.getTime());
                if(viewDropDownMonthRepeat != null)
                    setDropDownMonth(viewDropDownMonthRepeat);
                checkTime(timeStart, timeEnd, format, imgError, b);
            }
        };
        timeStart.addTextChangedListener(watcher);
        timeEnd.addTextChangedListener(watcher);
    }

    //Check valid time
    private boolean checkTime(TextView timeStart, TextView timeEnd, String format, ImageView imgError, boolean b) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        try {
            Date dayStart = sdf.parse(timeStart.getText().toString());
            Date dayEnd = sdf.parse(timeEnd.getText().toString());
            //Check if repeat or repeat n time
            if(b) {
                //Check begin day > end day
                if(dayStart.getTime() > dayEnd.getTime()) {
                    imgError.setVisibility(View.VISIBLE);
                    return false;
                } else {
                    imgError.setVisibility(View.GONE);
                    return true;
                }
            } else {
                imgError.setVisibility(View.GONE);
                return true;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    //Choose time and display
    private void handleClickTime(TextView time, LinearLayout containerTime, Calendar calendar, boolean b) {
        if(b)
            calendar.add(Calendar.MINUTE, 45);
        int hourTime = calendar.get(Calendar.HOUR_OF_DAY);
        int minuteTime = calendar.get(Calendar.MINUTE);
        SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat(HOUR_FORMAT, Locale.getDefault());
        time.setText(simpleDateFormat1.format(calendar.getTime()));
        containerTime.setOnClickListener(view -> {
            TimePickerDialog pickerDialog = new TimePickerDialog(view.getContext(), R.style.MyDatePickerStyle
                    , (timePicker, i, i1) -> {
                calendar.set(day, month, year, i, i1);
                time.setText(simpleDateFormat1.format(calendar.getTime()));
            }, hourTime, minuteTime, true);
            pickerDialog.show();
        });
    }

    //Dropdown repeat day, week, month
    private void dropDownRepeat() {
        ArrayList<SniperHome> listName = new ArrayList<>();
        for(String name : spinnerList) {
            listName.add(new SniperHome(name));
        }
        SpinnerDropdown dropdown = new SpinnerDropdown(this, listName);
        Spinner spinner = binding.spnRepeat;
        spinner.setAdapter(dropdown);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                viewDropDownMonthRepeat = view;
                typeSpinnerRepeat = i;
                switch (i) {
                    case 0:
                        binding.containerDayRepeat.setVisibility(View.GONE);
                        binding.containerMonthReport.setVisibility(View.GONE);
                        break;
                    case 1:
                        binding.containerDayRepeat.setVisibility(View.VISIBLE);
                        binding.containerMonthReport.setVisibility(View.GONE);
                        break;
                    case 2:
                    default:
                        binding.containerDayRepeat.setVisibility(View.GONE);
                        binding.containerMonthReport.setVisibility(View.VISIBLE);
                        setDropDownMonth(view);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //Do nothing
            }
        });
    }

    //Dropdown tính năng lặp vào ngày x tháng sau hoặc là ngày thứ đầu tiên của tháng sau
    private void setDropDownMonth(View view) {
        ArrayList<SniperHome> listTitleMonth = new ArrayList<>();
        listTitleMonth.add(new SniperHome(getString(R.string.every_month_on) + dayInt));
        listTitleMonth.add(new SniperHome(dayS + getString(R.string.first_of_every_month)));
        dropdownMonth = new SpinnerDropdown(view.getContext(), listTitleMonth);
        spinnerRepeat = binding.spnRepeatMonth;
        spinnerRepeat.setAdapter(dropdownMonth);
        spinnerRepeat.setSelection(typeSpinnerMonth);
        spinnerRepeat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                typeSpinnerMonth = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //Do nothing
            }
        });
    }

    //Chọn loại lặp
    private void timeRepeat() {
        binding.rdbRepateTo.setChecked(true);
        binding.rdbRepateTo.setOnCheckedChangeListener((compoundButton, check) -> {
            if(check) {
                binding.rdbRepeatTime.setChecked(false);
                binding.containerDayEnd.setVisibility(View.VISIBLE);
                flagRdb = true;
                checkTime(binding.txtDayStart, binding.txtDayEnd, DATE_FORMAT, binding.imgErrorDay, true);
                chooseDay(flagRdb);
            } else {
                binding.containerDayEnd.setVisibility(View.GONE);
            }
        });
        binding.rdbRepeatTime.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b) {
                binding.rdbRepateTo.setChecked(false);
                binding.edtRepateTime.setEnabled(true);
                flagRdb = false;
                checkTime(binding.txtDayStart, binding.txtDayEnd, DATE_FORMAT, binding.imgErrorDay, false);
                chooseDay(flagRdb);
            } else {
                binding.edtRepateTime.setEnabled(false);
            }
        });
        chooseDay(flagRdb);
    }

    //Bật tắt tính năng repeat
    private void repeat() {
        binding.swRepeat.setOnCheckedChangeListener((compoundButton, check) -> {
            if(check) {
                flagRdb = true;
                binding.spnRepeat.setVisibility(View.VISIBLE);
                binding.rdgRepeat.setVisibility(View.VISIBLE);
                if(binding.rdbRepateTo.isChecked()) {
                    binding.containerDayEnd.setVisibility(View.VISIBLE);
                    binding.containerDayEnd.setVisibility(View.VISIBLE);
                    binding.txtDayEnd.setText(binding.txtDayStart.getText().toString());
                    checkTime(binding.txtDayStart, binding.txtDayEnd, DATE_FORMAT, binding.imgErrorDay, true);
                    chooseDay(true);
                }
            } else {
                flagRdb = false;
                binding.spnRepeat.setVisibility(View.GONE);
                binding.rdgRepeat.setVisibility(View.GONE);
                if(binding.rdbRepateTo.isChecked()) {
                    binding.containerDayEnd.setVisibility(View.GONE);
                    checkTime(binding.txtDayStart, binding.txtDayEnd, DATE_FORMAT, binding.imgErrorDay, false);
                    chooseDay(false);
                }
            }
        });
    }

    //Chọn ngày bắt đầu và kết thúc
    private void chooseDay(boolean check) {
        semesterDay(binding.containerDayStart, binding.txtDayStart, check, false);
        semesterDay(binding.containerDayEnd, binding.txtDayEnd, check, true);
    }

    //Chọn ngày
    private void semesterDay(LinearLayout containerDayStart, TextView txtDayStart, boolean check, boolean dayBegin) {
        SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        textWatcher(binding.txtDayStart, binding.txtDayEnd, DATE_FORMAT, binding.imgErrorDay, check);
        containerDayStart.setOnClickListener(view -> chooseSemester(txtDayStart, simpleDateFormat1, dayBegin));
    }

    //Chọn ngày và kiểm tra hợp lệ
    private void chooseSemester(TextView daySemester, SimpleDateFormat simpleDateFormat, boolean dayBegin) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, R.style.MyDatePickerStyle
                , (datePicker, i, i1, i2) -> {
            calendar.set(i, i1, i2);
            Date date = null;
            Calendar today = Calendar.getInstance();
            //Kiểm tra có phải ngày bắt đầu
            if(dayBegin) {
                try {
                    date = simpleDateFormat.parse(binding.txtDayStart.getText().toString());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                assert date != null;
                today.setTime(date);
                today.add(Calendar.YEAR, 3);
                //Kiểm tra ngày kết thúc có cách ngày bắt đầu tối đa 3 năm
                if(calendar.after(today)) {
                    today.add(Calendar.YEAR, -3);
                    daySemester.setText(simpleDateFormat.format(today.getTime()));
                    Toast.makeText(LessonCreate.this, getText(R.string.the_end_date), Toast.LENGTH_SHORT).show();
                } else {
                    daySemester.setText(simpleDateFormat.format(calendar.getTime()));
                }
            } else {
                try {
                    date = simpleDateFormat.parse(binding.txtDayEnd.getText().toString());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                assert date != null;
                today.setTime(date);
                today.add(Calendar.YEAR, -3);
                //Kiểm tra ngày bắt đầu có cách kết thúc tối đa 3 năm
                if(today.after(calendar)) {
                    today.add(Calendar.YEAR, 3);
                    daySemester.setText(simpleDateFormat.format(today.getTime()));
                    Toast.makeText(LessonCreate.this, getString(R.string.the_start_date), Toast.LENGTH_SHORT).show();
                } else {
                    daySemester.setText(simpleDateFormat.format(calendar.getTime()));
                }
            }
        }, year, month, day);
        datePickerDialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}