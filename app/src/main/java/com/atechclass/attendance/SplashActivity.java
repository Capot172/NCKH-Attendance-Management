package com.atechclass.attendance;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.atechclass.attendance.OnBroading.SlideLayout;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import de.hdodenhof.circleimageview.CircleImageView;

public class SplashActivity extends AppCompatActivity {
    Animation topAnim;
    Animation bottomAnim;
    Animation middleAnim;
    private ViewPager slide_Viewpaper;
    private LinearLayout mDolayout;
    private TextView[] mDots;
    private SlideLayout slide_layout;
    RelativeLayout OnBoradingLayout, SplashLayout;
    private CircleImageView next;
    Button skip;
    Language language;
    private  int mcurrent;
    private static final String ONBROAD = "onbroad";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);
        language = new Language(this);
        language.Language();

        SharedPreferences preferences = getSharedPreferences(ONBROAD, MODE_PRIVATE);
        boolean introduce = preferences.getBoolean("Introduce", true);
        SharedPreferences.Editor editor = preferences.edit();

        init();

        if(introduce ){
            OnBoradingLayout.setVisibility(View.VISIBLE);
            SplashLayout.setVisibility(View.GONE);
            editor.putBoolean("Introduce", false);
            editor.commit();
        }
        else{
            OnBoradingLayout.setVisibility(View.GONE);
            SplashLayout.setVisibility(View.VISIBLE);
            CheckLogin();
        }
        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SplashActivity.this,Login.class));
            }
        });

    }

    private void CheckLogin() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if(account!=null){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            topAnim = AnimationUtils.loadAnimation(this, R.anim.slide_down);
            bottomAnim = AnimationUtils.loadAnimation(this, R.anim.bottom_up);
            middleAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in);

            final ImageView imgTop = findViewById(R.id.img_splash_top);
            final ImageView imgBottom = findViewById(R.id.img_splash_bottom);
            final ImageView logo = findViewById(R.id.img_splash_logo);
            final ImageView dotLeft = findViewById(R.id.dot_left);
            final ImageView dotRight = findViewById(R.id.dot_right);
            final TextView txtAttendance = findViewById(R.id.txt_atttendance);
            final TextView txtManage = findViewById(R.id.manager);

            imgTop.setAnimation(topAnim);
            imgBottom.setAnimation(bottomAnim);
            logo.setAnimation(middleAnim);
            dotLeft.setAnimation(middleAnim);
            dotRight.setAnimation(middleAnim);
            txtAttendance.setAnimation(middleAnim);
            txtManage.setAnimation(middleAnim);

            new Handler().postDelayed(() -> {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }, 2000);
        } else {
            startActivity(new Intent(SplashActivity.this, Login.class));
        }
    }

    @Override
    public void recreate() {
        super.recreate();
    }

    void init(){
        slide_Viewpaper = findViewById(R.id.view_pager);
        mDolayout = findViewById(R.id.dotshot);
        next=findViewById(R.id.button3);
        slide_layout = new SlideLayout(this);
        slide_Viewpaper.setAdapter(slide_layout);
        addDot(0);
        slide_Viewpaper.addOnPageChangeListener(viewListner);
        skip = findViewById(R.id.btn_skip);
        SplashLayout = findViewById(R.id.splash);
        OnBoradingLayout = findViewById(R.id.onbroading);
    }


    public void addDot(int position){
        mDots = new TextView[3];
        mDolayout.removeAllViews();

        for (int i=0;i<3;i++)
        {
            mDots[i] = new TextView(this);
            mDots[i].setText(Html.fromHtml("&#8226;"));
            mDots[i].setTextSize(45);
            mDots[i].setTextColor(getResources().getColor(R.color.color_status_bar));
            mDolayout.addView(mDots[i]);
        }
        if(mDots.length>0)
            mDots[position].setTextColor(getResources().getColor(R.color.white));
    }

    ViewPager.OnPageChangeListener viewListner = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            addDot(position);
            mcurrent = position;

            if (mcurrent == 2) {
                next.setEnabled(true);
                next.setVisibility(View.VISIBLE);
                next.setImageResource(R.drawable.next);

                next.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent= new Intent(SplashActivity.this, SplashActivity.class);
                        startActivity(intent);
                    }
                });
            } else {
                next.setEnabled(false);
                next.setVisibility(View.GONE);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };
}