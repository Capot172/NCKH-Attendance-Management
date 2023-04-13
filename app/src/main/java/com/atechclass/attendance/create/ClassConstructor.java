//package com.example.atendance.create;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.appcompat.widget.AppCompatButton;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import android.app.AlertDialog;
//import android.app.DatePickerDialog;
//import android.app.Dialog;
//import android.app.TimePickerDialog;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.os.Bundle;
//import android.text.Editable;
//import android.text.TextWatcher;
//import android.util.Log;
//import android.view.View;
//import android.view.Window;
//import android.widget.CheckBox;
//import android.widget.DatePicker;
//import android.widget.EditText;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.Switch;
//import android.widget.TextView;
//import android.widget.TimePicker;
//import android.widget.Toast;
//
//import com.example.atendance.OnClickItem;
//import com.example.atendance.R;
//import com.example.atendance.adapter.DayOffAdapter;
//import com.example.atendance.adapter.SubClassAdapter;
//import com.example.atendance.model.Semester;
//import com.example.atendance.object.DayTime;
//import com.example.atendance.object.Lesson;
//import com.example.atendance.object.Subject;
//import com.example.atendance.object.TimeStudy;
//import com.example.atendance.ultis.HolidayUtils;
//import com.example.atendance.ultis.UserLogin;
//import com.google.android.gms.tasks.OnCompleteListener;
//import com.google.android.gms.tasks.OnFailureListener;
//import com.google.android.gms.tasks.Task;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.firestore.FirebaseFirestore;
//
//import java.text.DateFormat;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.List;
//import java.util.Locale;
//
//public class ClassConstructor extends AppCompatActivity implements OnClickItem, DayOffAdapter.OnClickDayOff {
//    RecyclerView rcvTableTime, rcvDayOff;
//    SubClassAdapter subClassAdapter;
//    DayOffAdapter dayOffAdapter;
//    ArrayList listDayOff;
//    ArrayList<Semester> listSemester;
//    TextView txtStartDay, txtEndDay, txtSubject;
//    ImageView btnAddTableTime, btnBackCreate, imgErrorSemester;
//    LinearLayout btnAddDayOff, btnStartDay, btnEndDay;
//    Switch holidayOff;
//
//    final Calendar calendar = Calendar.getInstance();
//    int day = calendar.get(Calendar.DAY_OF_MONTH);
//    int month = calendar.get(Calendar.MONTH);
//    int year = calendar.get(Calendar.YEAR);
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_class_constructor);
//
//        ORM();
//
//        LinearLayoutManager linearLayoutManager1 = new LinearLayoutManager(this);
//        LinearLayoutManager linearLayoutManager2 = new LinearLayoutManager(this);
//        rcvTableTime.setLayoutManager(linearLayoutManager1);
//        rcvDayOff.setLayoutManager(linearLayoutManager2);
//        listDayOff = new ArrayList();
//        dayOffAdapter = new DayOffAdapter(listDayOff, this, ClassConstructor.this);
//        listSemester = new ArrayList<>();
//        subClassAdapter = new SubClassAdapter(listSemester, ClassConstructor.this, this);
//        rcvDayOff.setAdapter(dayOffAdapter);
//
//
//        addTableTime();
//        addDayOff();
//        semesterDay(btnStartDay, txtStartDay);
//        semesterDay(btnEndDay, txtEndDay);
//        subjectEmpty();
//    }
//
//    private void subjectEmpty() {
//        txtSubject.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                if(charSequence.length() <= 0)
//                    txtSubject.setError("Tên môn học không được để trống");
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//            }
//        });
//    }
//
//    private void semesterDay(LinearLayout btnDay, TextView txtDay) {
//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM, yyyy");
//        txtDay.setText(simpleDateFormat.format(calendar.getTime()));
//        textWatcher(txtStartDay, txtEndDay, "EEE, dd MMM, yyyy", imgErrorSemester, false);
//        btnDay.setOnClickListener(view -> {
//            chooseSemester(txtDay, simpleDateFormat);
//        });
//    }
//
//    private void chooseSemester(TextView daySemester, SimpleDateFormat simpleDateFormat) {
//        DatePickerDialog datePickerDialog = new DatePickerDialog(this, R.style.MyDatePickerStyle, new DatePickerDialog.OnDateSetListener() {
//            @Override
//            public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
//                calendar.set(i, i1, i2);
//                daySemester.setText(simpleDateFormat.format(calendar.getTime()));
//            }
//        }, year, month, day);
//        datePickerDialog.show();
//    }
//
//    private void addDayOff() {
//        btnAddDayOff.setOnClickListener(view -> {
//            calender(day, month, year, calendar);
//        });
//    }
//
//    private void calender(int day, int month, int year, Calendar calendar) {
//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM, yyyy");
//        DatePickerDialog pickerDialog = new DatePickerDialog(this, R.style.MyDatePickerStyle , (datePicker, i, i1, i2) -> {
//            calendar.set(i, i1, i2);
//            for(int j = 0; j < listDayOff.size(); j++) {
//                if(simpleDateFormat.format(calendar.getTime()).equals(dayOffAdapter.getDateTime(j))) {
//                    Toast.makeText(this, "Đã có ngày này rồi!", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//            }
//            Date date = null;
//            try {
//                date = simpleDateFormat.parse(simpleDateFormat.format(calendar.getTime()));
//            } catch (ParseException e) {
//                e.printStackTrace();
//            }
//            Calendar calendarDay = Calendar.getInstance();
//            calendarDay.setTime(date);
//            listDayOff.add(0, new DayTime(simpleDateFormat.format(calendarDay.getTime()), calendarDay.getTime().toString()));
//            dayOffAdapter.setListItem(listDayOff);
//        }, year, month, day);
//        pickerDialog.show();
//    }
//
//    private void addTableTime() {
//        btnAddTableTime.setOnClickListener(view -> {
//            Dialog dialog = new Dialog(this);
//            dialogTimeTable(dialog);
//            confirm(dialog);
//        });
//    }
//
//    private void dialogTimeTable(Dialog dialog) {
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        dialog.setCancelable(false);
//        dialog.setContentView(R.layout.layout_add_lesson);
//        dialog.getWindow().setBackgroundDrawableResource(R.drawable.cus_dialog);
//        dialog.setCancelable(true);
//        initWidgetsDialog(dialog);
//        handleClickTime(txtStartLesson);
//        handleClickTime(txtEndLesson);
//        dialog.show();
//        textWatcher(txtStartLesson, txtEndLesson, "HH:mm", imgError, true);
//        cancel(dialog);
//    }
//
//    private void confirm(Dialog dialog) {
//        btnCreateLesson.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                String[] days = dayOfWeek().toArray(new String[dayOfWeek().size()]);
//                if(days.length > 0 && edtRoom.length() > 0 && edtAddress.length() > 0) {
//                    listSemester.add(new Semester(txtStartLesson.getText().toString(), txtEndLesson.getText().toString(),
//                            dayOfWeek(), edtRoom.getText().toString(), edtAddress.getText().toString()));
//                    subClassAdapter.setSemesterList(listSemester);
//                    rcvTableTime.setAdapter(subClassAdapter);
//                    dialog.dismiss();
//                } else {
//                    Toast.makeText(ClassConstructor.this, "Vui nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
//    }
//
//    private void edit(Dialog dialog, int position) {
//        btnCreateLesson.setText("Xác nhận");
//        btnCreateLesson.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                String[] days = dayOfWeek().toArray(new String[dayOfWeek().size()]);
//                if(days.length > 0) {
//                    listSemester.get(position).setTimeStart(txtStartLesson.getText().toString());
//                    listSemester.get(position).setTimeEnd(txtEndLesson.getText().toString());
//                    listSemester.get(position).setWeeks(dayOfWeek());
//                    subClassAdapter.setSemesterList(listSemester);
//                    subClassAdapter.setSemesterList(listSemester);
//                    rcvTableTime.setAdapter(subClassAdapter);
//                    dialog.dismiss();
//                } else {
//                    Toast.makeText(ClassConstructor.this, "Vui lòng chọn một ngày trong tuần!", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
//    }
//
//    private ArrayList<String> dayOfWeek() {
//        ArrayList<String> daysList = new ArrayList();
//        if(cbMon.isChecked())
//            daysList.add(cbMon.getText().toString());
//        if(cbTue.isChecked())
//            daysList.add(cbTue.getText().toString());
//        if(cbWed.isChecked())
//            daysList.add(cbWed.getText().toString());
//        if(cbThu.isChecked())
//            daysList.add(cbThu.getText().toString());
//        if(cbFri.isChecked())
//            daysList.add(cbFri.getText().toString());
//        if(cbSat.isChecked())
//            daysList.add(cbSat.getText().toString());
//        if(cbSun.isChecked())
//            daysList.add(cbSun.getText().toString());
//        return daysList;
//    }
//
//    private void cancel(Dialog dialog) {
//        btnCancelLesson.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                dialog.dismiss();
//            }
//        });
//    }
//
//    private void textWatcher(TextView txtStart, TextView txtEnd, String format, ImageView imgError, boolean b) {
//        TextWatcher watcher = new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                if(txtStart.getText().toString() != null && txtEnd.getText().toString() != null) {
//                    checkTime(txtStart, txtEnd, format, imgError, b);
//                }
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//                if(txtStart.getText().toString() != null && txtEnd.getText().toString() != null) {
//                    checkTime(txtStart, txtEnd, format, imgError, b);
//                }
//            }
//        };
//        txtStart.addTextChangedListener(watcher);
//        txtEnd.addTextChangedListener(watcher);
//    }
//
//    private void checkTime(TextView txtStart, TextView txtEnd, String format, ImageView imgError, boolean b) {
//        SimpleDateFormat sdf = new SimpleDateFormat(format);
//        try {
//            Date date1 = sdf.parse(txtStart.getText().toString());
//            Date date2 = sdf.parse(txtEnd.getText().toString());
//            if(date1.getTime() > date2.getTime()) {
//                imgError.setVisibility(View.VISIBLE);
//                if(b) {
//                    btnCreateLesson.setEnabled(false);
//                    btnCreateLesson.setTextColor(getResources().getColor(R.color.color_grey));
//                }
//            } else {
//                imgError.setVisibility(View.GONE);
//                if(b) {
//                    btnCreateLesson.setEnabled(true);
//                    btnCreateLesson.setTextColor(getResources().getColor(R.color.black));
//                }
//            }
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void handleClickTime(TextView mTxtLesson) {
//        Calendar calendar = Calendar.getInstance();
//        int hour = calendar.get(Calendar.HOUR_OF_DAY);
//        int minute = calendar.get(Calendar.MINUTE);
//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
//        mTxtLesson.setText(simpleDateFormat.format(calendar.getTime()));
//        mTxtLesson.setOnClickListener(view -> {
//            TimePickerDialog pickerDialog = new TimePickerDialog(view.getContext(), R.style.MyDatePickerStyle, new TimePickerDialog.OnTimeSetListener() {
//                @Override
//                public void onTimeSet(TimePicker timePicker, int i, int i1) {
//                    calendar.set(0, 0, 0, i, i1);
//                    mTxtLesson.setText(simpleDateFormat.format(calendar.getTime()));
//                }
//            }, hour, minute, true);
//            pickerDialog.show();
//        });
//    }
//
//    private TextView txtStartLesson, txtEndLesson;
//    private ImageView imgError;
//    private AppCompatButton btnCreateLesson, btnCancelLesson;
//    private CheckBox cbMon, cbTue, cbWed, cbThu, cbFri, cbSat, cbSun;
//    private EditText edtRoom, edtAddress;
//    private void initWidgetsDialog(Dialog dialog) {
//        txtStartLesson = dialog.findViewById(R.id.txt_time_study_start);
//        txtEndLesson = dialog.findViewById(R.id.txt_time_study_end);
//        imgError = dialog.findViewById(R.id.notice_error);
//        btnCreateLesson = dialog.findViewById(R.id.btn_create_lesson);
//        btnCancelLesson = dialog.findViewById(R.id.btn_cancel_lesson);
//
//        cbMon = dialog.findViewById(R.id.cb_mon);
//        cbTue = dialog.findViewById(R.id.cb_tue);
//        cbWed = dialog.findViewById(R.id.cb_wed);
//        cbThu = dialog.findViewById(R.id.cb_thu);
//        cbFri = dialog.findViewById(R.id.cb_fri);
//        cbSat = dialog.findViewById(R.id.cb_sat);
//        cbSun = dialog.findViewById(R.id.cb_sun);
//
//        edtRoom = dialog.findViewById(R.id.txt_room);
//        edtAddress = dialog.findViewById(R.id.txt_address);
//    }
//
//    private void ORM() {
//        rcvTableTime = findViewById(R.id.rcvTableTime);
//        rcvDayOff = findViewById(R.id.rcv_day_off);
//        btnAddTableTime = findViewById(R.id.btnCrateTableTime);
//        btnBackCreate = findViewById(R.id.backCreate);
//        btnAddDayOff = findViewById(R.id.btn_add_day_off);
//        btnStartDay = findViewById(R.id.btn_start_day);
//        btnEndDay = findViewById(R.id.btn_end_day);
//        txtStartDay = findViewById(R.id.txt_start_day);
//        txtEndDay = findViewById(R.id.txt_end_day);
//        txtSubject = findViewById(R.id.txt_subject);
//        imgErrorSemester = findViewById(R.id.ic_error);
//        holidayOff = findViewById(R.id.sw_off_holiday);
//    }
//
//    @Override
//    public void deleteItem(int position) {
//        listSemester.remove(position);
//    }
//
//    @Override
//    public void editItem(int position) {
//        Dialog dialog = new Dialog(this);
//        dialogTimeTable(dialog);
//        edit(dialog, position);
//    }
//
//    ArrayList<TimeStudy> listStudyDays = new ArrayList<TimeStudy>();
//    int x = 1;
//    @Override
//    public void getData(String[] listData, String startTime, String endTime, String address, String room) {
//        if(listStudyDays.size() > x) {
//            for (int i = 0 ; i < x; i++) {
//                listStudyDays.remove(0);
//            }
//            x++;
//        }
//        listStudyDays.add(new TimeStudy(listData, startTime, endTime, address, room));
//    }
//
//    ArrayList<String> listDaysOff = new ArrayList<>();
//    int x2 = 1;
//    @Override
//    public void getDaysOff(String day, boolean delete, int position) {
//        if(listDaysOff.size() > 0 && delete) {
//            for (int i = 0 ; i < listDaysOff.size(); i++) {
//                if(listDaysOff.get(i).equals(day)) {
//                    listDaysOff.remove(i);
//                    x2--;
//                }
//            }
//        } else {
//            if(listDaysOff.size() > x2) {
//                for (int i = 0 ; i < x2; i++) {
//                    listDaysOff.remove(0);
//                }
//                x2++;
//            }
//            listDaysOff.add(day);
//        }
//    }
//
//    public void onBackHome(View view) {
//        onBackPressed();
//    }
//
//    public void doneCreate(View view) {
//        if(txtSubject.getText().length() == 0)
//            txtSubject.setError("Vui lòng nhập tên môn học");
//        else {
//            caclutator(txtStartDay, txtEndDay);
//        }
//    }
//
//    private void caclutator(TextView txtStartDay, TextView txtEndDay) {
//        ArrayList<String> list = new ArrayList<>();
//        Subject subject = new Subject(txtSubject.getText().toString());
//        DateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM, yyyy", Locale.getDefault());
//        Date date1 = null;
//        Date date2 = null;
//        try {
//            date1 = simpleDateFormat.parse(txtStartDay.getText().toString());
//            date2 = simpleDateFormat.parse(txtEndDay.getText().toString());
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//        Calendar calendar1 = Calendar.getInstance();
//        calendar1.setTime(date1);
//        Calendar calendar2 = Calendar.getInstance();
//        calendar2.setTime(date2);
//
//        while (!calendar1.after(calendar2)) {
//            String haha = simpleDateFormat.format(calendar1.getTime());
//            list.add(haha);
//            calendar1.add(Calendar.DATE, 1);
//        }
//        ArrayList<Lesson> subjects = new ArrayList<>();
//        boolean flag = true;
//        int flagObject = 1;
//        for(String str1 : list) {
//            if(holidayOff.isChecked())
//                for(String holidays : HolidayUtils.getHoliday())
//                    if (str1.contains(holidays))
//                        flag = false;
//            if(!flag) {
//                flag = true;
//            } else {
//                for (TimeStudy time : listStudyDays) {
//                    for (String str : time.getDays()) {
//                        if (str1.contains(str)) {
//                            try {
//                                subjects.add(new Lesson(str1, txtSubject.getText().toString(), time.getStartTime(),
//                                        time.getEndTime(), time.getAddress(), time.getRoom(), subject.getId(), flagObject));
//                            } catch (ParseException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        if(subjects.size() > 1) {
//            for(int i = 0; i < subjects.size() ; i++) {
//                for(int j = i + 1; j < subjects.size(); j++) {
//                    if(subjects.get(i).getDayTime().equals(subjects.get(j).getDayTime())
//                            && subjects.get(i).getStartTime().equals(subjects.get(j).getStartTime())
//                            && subjects.get(i).getEndTime().equals(subjects.get(j).getEndTime())) {
//                        subjects.remove(j);
//                        j--;
//                    }
//                }
//            }
//        }
//
//        if(listDaysOff.size() > 0 && subjects.size() > 0) {
//            for(int i = 0 ; i < listDaysOff.size(); i++) {
//                for (int j = 0; j < subjects.size(); j++) {
//                    if(subjects.get(j).getDayTime().equals(listDaysOff.get(i))) {
//                        subjects.remove(j);
//                        j--;
//                    }
//                }
//            }
//        }
//
//        if(subjects.size() == 0) {
//            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
//            dialog.setTitle("Không có buổi học");
//            dialog.setMessage("Dữ liệu môn học chưa có, bạn có muốn tiếp tục?")
//                    .setPositiveButton("Có", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialogInterface, int i) {
//                            dialogInterface.dismiss();
//                            onBackPressed();
//                            finish();
//                        }
//                    })
//                    .setNegativeButton("Không", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialogInterface, int i) {
//                            dialogInterface.dismiss();
//                        }
//                    }).show();
//        } else {
//            FirebaseFirestore db = FirebaseFirestore.getInstance();
//            FirebaseAuth auth = UserLogin.getAuth();
//            FirebaseUser dbUser = auth.getCurrentUser();
//
//
//            for(Lesson lesson : subjects)
//                db.collection("User").document(dbUser.getUid())
//                        .collection("Subjects").document(subject.getId()+"")
//                                .set(subject);
//            for (Lesson lesson : subjects) {
//                db.collection("User").document(dbUser.getUid())
//                        .collection("Subjects").document(subject.getId()+"")
//                        .collection("Lesson").document(lesson.getId() + "")
//                        .set(lesson);
//            }
//            Intent notice = new Intent();
//            notice.putExtra("create_subject", true);
//            setResult(RESULT_OK, notice);
//            finish();
//        }
//    }
//}
