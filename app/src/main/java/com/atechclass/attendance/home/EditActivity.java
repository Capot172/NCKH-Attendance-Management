package com.atechclass.attendance.home;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.atechclass.attendance.Language;
import com.atechclass.attendance.R;
import com.atechclass.attendance.model.UserUpdate;
import com.atechclass.attendance.ultis.UserLogin;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditActivity extends AppCompatActivity {
    private Toolbar acbEdit;
    Language language;

    private EditText edtSubject;
    private EditText edtRoom;
    private EditText edtAddress;

    private TextView txtTime;
    private TextView txtTimeStart;
    private TextView txtTimeEnd;

    private ImageView imgError;

    private Intent intent;
    private long id;
    private boolean checkTime = true;

    Date date = null;
    Date date2 = null;
    DateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM, yyyy", Locale.getDefault());
    DateFormat simpleDateFormat2 = new SimpleDateFormat("HH:mm");
    Calendar calendar = Calendar.getInstance();
    Calendar calendar2 = Calendar.getInstance();
    int day = calendar.get(Calendar.DAY_OF_MONTH);
    int month = calendar.get(Calendar.MONTH);
    int year = calendar.get(Calendar.YEAR);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        language = new Language(this);
        language.Language();
        setContentView(R.layout.activity_edit);
        initWidgets();
        setSupportActionBar(acbEdit);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);

        intent = getIntent();
        setData(intent);
        pickTime();
        LessonDay();
    }

    private void pickTime() {
        handleClickTime(txtTimeStart);
        handleClickTime(txtTimeEnd);
        textWatcher(txtTimeStart, txtTimeEnd, "HH:mm", imgError);
    }

    private void textWatcher(TextView txtStart, TextView txtEnd, String format, ImageView imgError) {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(txtStart.getText().toString() != null && txtEnd.getText().toString() != null) {
                    checkTime(txtStart, txtEnd, format, imgError);
                }
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(txtStart.getText().toString() != null && txtEnd.getText().toString() != null) {
                    checkTime(txtStart, txtEnd, format, imgError);
                }
            }
        };
        txtStart.addTextChangedListener(watcher);
        txtEnd.addTextChangedListener(watcher);
    }

    private void checkTime(TextView txtStart, TextView txtEnd, String format, ImageView imgError) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            Date date1 = sdf.parse(txtStart.getText().toString());
            Date date2 = sdf.parse(txtEnd.getText().toString());
            if(date1.getTime() > date2.getTime()) {
                imgError.setVisibility(View.VISIBLE);
                checkTime = false;
            } else {
                imgError.setVisibility(View.GONE);
                checkTime = true;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void handleEdit() {
        if(edtSubject.length() == 0 && edtRoom.length() == 0 && edtAddress.length() == 0 && checkTime == false) {
            Toast.makeText(this, getString(R.string.pls_enter_information), Toast.LENGTH_SHORT).show();
        } else {
            try {
                date = simpleDateFormat.parse(txtTime.getText().toString());
                calendar.setTime(date);
                Log.e("AAA", calendar.getTimeInMillis() + "");
                date2 = simpleDateFormat2.parse(txtTimeStart.getText().toString());
                calendar2.setTime(date2);
                id = calendar.getTimeInMillis() + (long) calendar2.getTime().getHours() + (long) calendar2.getTime().getMinutes();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            firestore.collection("User").document(UserLogin.getAuth().getUid())
                    .collection("Subjects").document(String.valueOf(intent.getLongExtra("user_id_subject", 0)))
                    .collection("Lesson").document(String.valueOf(intent.getLongExtra("user_id", 0))).delete();
            firestore.collection("User").document(UserLogin.getAuth().getUid())
                    .collection("Subjects").document(String.valueOf(intent.getLongExtra("user_id_subject", 0)))
                    .collection("Lesson").document(String.valueOf(id))
                    .set(new UserUpdate(txtTime.getText().toString(), edtSubject.getText().toString()
                            , txtTimeStart.getText().toString(), txtTimeEnd.getText().toString(),
                            edtAddress.getText().toString(), edtRoom.getText().toString(), id
                    , intent.getLongExtra("user_id_subject", 0), intent.getIntExtra("user_flag", -1)));
        }
    }

    private void LessonDay() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM, yyyy");
        txtTime.setText(simpleDateFormat.format(calendar.getTime()));
        txtTime.setOnClickListener(view -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(this, R.style.MyDatePickerStyle, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                    calendar.set(i, i1, i2);
                    txtTime.setText(simpleDateFormat.format(calendar.getTime()));
                }
            }, year, month, day);
            datePickerDialog.show();
        });
    }

    private void handleClickTime(TextView time) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        time.setText(simpleDateFormat.format(calendar.getTime()));
        time.setOnClickListener(view -> {
            TimePickerDialog pickerDialog = new TimePickerDialog(view.getContext(), R.style.MyDatePickerStyle, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int i, int i1) {
                    calendar.set(0, 0, 0, i, i1);
                    time.setText(simpleDateFormat.format(calendar.getTime()));
                }
            }, hour, minute, true);
            pickerDialog.show();
        });
    }

    private void setData(Intent intent) {
        edtSubject.setText(intent.getStringExtra("user_subject"));
        edtRoom.setText(intent.getStringExtra("user_room"));
        edtAddress.setText(intent.getStringExtra("user_address"));
        txtTime.setText(intent.getStringExtra("user_daytime"));
        txtTimeStart.setText(intent.getStringExtra("user_starttime"));
        txtTimeEnd.setText(intent.getStringExtra("user_endtime"));

    }

    private void initWidgets() {
        acbEdit = findViewById(R.id.acb_list_class);
        edtSubject = findViewById(R.id.txt_subject);
        edtRoom = findViewById(R.id.txt_room_show);
        edtAddress = findViewById(R.id.txt_address_show);
        txtTime = findViewById(R.id.txt_day_study);
        txtTimeStart = findViewById(R.id.txt_time_study_start);
        txtTimeEnd = findViewById(R.id.txt_time_study_end);
        imgError = findViewById(R.id.notice_error);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_done, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_menu_done:
                handleEdit();
                Intent notice = new Intent();
                notice.putExtra("create_subject", true);
                setResult(RESULT_OK, notice);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}