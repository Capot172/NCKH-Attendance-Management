package com.atechclass.attendance.function;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.atechclass.attendance.Language;
import com.atechclass.attendance.R;

public class Feedback extends AppCompatActivity {
    ImageButton btnBack;
    Button submitFeedback;
    Language language;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        language = new Language(this);
        language.Language();

        setContentView(R.layout.activity_feedback);
        ORM();
        btnBack.setOnClickListener(view -> finish());

        submitFeedback.setOnClickListener(view -> submitFeedback());
    }

    private void ORM() {
        btnBack = findViewById(R.id.btnBack);
        submitFeedback = findViewById(R.id.txtSubmit);
    }

    public void submitFeedback(){
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        String EmailList = "nnhuquynh2603@gmail.com";
        emailIntent.setData(Uri.parse("mailto:" + EmailList + "?subject=" + "Attendance Feedback "));
        emailIntent.putExtra(Intent.EXTRA_EMAIL, EmailList);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Attendance Feedback");
        try{
            startActivityForResult(Intent.createChooser(emailIntent,"Send Email"), 1000);
        } catch (android.content.ActivityNotFoundException ex){
            Toast.makeText(this, "Email not installed" , Toast.LENGTH_SHORT).show();
        }
    }
}