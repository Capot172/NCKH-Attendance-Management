package com.atechclass.attendance.function;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageButton;

import com.atechclass.attendance.Language;
import com.atechclass.attendance.R;

public class Policy extends AppCompatActivity {
    ImageButton btnBack;
    Language language;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_policy);

        btnBack = findViewById(R.id.backP);
        btnBack.setOnClickListener(view -> finish());
    }


}