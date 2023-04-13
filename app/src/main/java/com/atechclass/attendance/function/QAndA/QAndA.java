package com.atechclass.attendance.function.QAndA;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.atechclass.attendance.Language;
import com.atechclass.attendance.R;

public class QAndA extends AppCompatActivity {
    private ImageButton btnBack;
    TextView txtQA1, txtQA2, txtQA3, txtQA4, txtQA5, txtQA6, txtQA7, txtQA8;
    RelativeLayout btnQA1, btnQA2, btnQA3, btnQA4, btnQA5, btnQA6, btnQA7, btnQA8;
    ImageView imgQA1, imgQA2, imgQA3, imgQA4, imgQA5, imgQA6, imgQA7, imgQA8;
    boolean check1 = true, check2 = true, check3 = true, check4 = true, check5 = true, check6 = true, check7 = true, check8 = true ;
    Language language;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        language = new Language(this);
        language.Language();
        setContentView(R.layout.activity_qand);
        ORM();
        btnBack.setOnClickListener(view -> finish());

        ShowQA1();
    }


    private boolean openContent(TextView txtQA, ImageView imgQA) {
        txtQA.setVisibility(View.VISIBLE);
        imgQA.setImageResource(R.drawable.ic_down);
        return false;
    }

    private boolean closeContent(TextView txtQA, ImageView imgQA) {
        txtQA.setVisibility(View.GONE);
        imgQA.setImageResource(R.drawable.ic_top);
        return true;
    }
    private void ShowQA1(){
        btnQA1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(check1) {
                    check1 = openContent(txtQA1, imgQA1);
                } else {
                    check1 = closeContent(txtQA1, imgQA1);
                }
            }
        });

        btnQA2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(check2) check2 = openContent(txtQA2,imgQA2);
                else check2 = closeContent(txtQA2,imgQA2);
            }
        });

        btnQA3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(check3) check3 = openContent(txtQA3,imgQA3);
                else check3 = closeContent(txtQA3,imgQA3);
            }
        });
        

        btnQA4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(check4) check4 = openContent(txtQA4,imgQA4);
                else check4 = closeContent(txtQA4,imgQA4);
            }
        });

        btnQA5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(check5) check5 = openContent(txtQA5,imgQA5);
                else check5 = closeContent(txtQA5,imgQA5);
            }
        });

        btnQA6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(check6) check6 = openContent(txtQA6,imgQA6);
                else check6 = closeContent(txtQA6,imgQA6);
            }
        });

        btnQA7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(check7) check7 = openContent(txtQA7,imgQA7);
                else check7 = closeContent(txtQA7,imgQA7);
            }
        });

        btnQA8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(check8) check8 = openContent(txtQA8,imgQA8);
                else check8 = closeContent(txtQA8,imgQA8);
            }
        });
    }
    private void ORM() {
        btnBack = findViewById(R.id.back);

        btnQA1 = findViewById(R.id.btn_QA1);
        txtQA1 = findViewById(R.id.txt_QA1);
        imgQA1 = findViewById(R.id.img_QA1);

        btnQA2 = findViewById(R.id.btn_QA2);
        txtQA2 = findViewById(R.id.txt_QA2);
        imgQA2 = findViewById(R.id.img_QA2);

        btnQA3 = findViewById(R.id.btn_QA3);
        txtQA3 = findViewById(R.id.txt_QA3);
        imgQA3 = findViewById(R.id.img_QA3);

        btnQA4 = findViewById(R.id.btn_QA4);
        txtQA4 = findViewById(R.id.txt_QA4);
        imgQA4 = findViewById(R.id.img_QA4);

        btnQA5 = findViewById(R.id.btn_QA5);
        txtQA5 = findViewById(R.id.txt_QA5);
        imgQA5 = findViewById(R.id.img_QA5);

        btnQA6 = findViewById(R.id.btn_QA6);
        txtQA6 = findViewById(R.id.txt_QA6);
        imgQA6 = findViewById(R.id.img_QA6);

        btnQA7 = findViewById(R.id.btn_QA7);
        txtQA7 = findViewById(R.id.txt_QA7);
        imgQA7 = findViewById(R.id.img_QA7);

        btnQA8 = findViewById(R.id.btn_QA8);
        txtQA8 = findViewById(R.id.txt_QA8);
        imgQA8 = findViewById(R.id.img_QA8);
    }
}