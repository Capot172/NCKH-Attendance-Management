package com.atechclass.attendance.function.class_list;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;
import android.view.MenuItem;

import com.atechclass.attendance.Language;
import com.atechclass.attendance.R;
import com.atechclass.attendance.adapter.ClassListAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ClassListItem extends AppCompatActivity {
    private BottomNavigationView navigationView;
    private ViewPager2 viewSlideList;
    private ClassListAdapter classListAdapter;
    private Toolbar acbListClass;
    Language language;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_list_item);
        language = new Language(this);
        language.Language();
        initWidgets();
        setSupportActionBar(acbListClass);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);

        setFragmentResultListeners();

        initViewPager();
        eventNavBottom();
    }

    private void setFragmentResultListeners() {
        getSupportFragmentManager().setFragmentResultListener("create", this, (requestKey, bundle) -> {
            boolean result = bundle.getBoolean("hasChanged");
            if (result){
                setResult(RESULT_OK);
            }
        });

        getSupportFragmentManager().setFragmentResultListener("delete", this, (requestKey, bundle) -> {
            boolean result = bundle.getBoolean("hasChanged");
            if (result){
                setResult(RESULT_OK);
            }
        });

        getSupportFragmentManager().setFragmentResultListener("goToAdd", this, (requestKey, bundle) -> {
            boolean result = bundle.getBoolean("goToAdd");
            if (result){
                Bundle open = new Bundle();
                open.putBoolean("openAdd", true);
                getSupportFragmentManager().setFragmentResult("openAdd", open);

                navigationView.getMenu().findItem(R.id.menu_lessions).setChecked(true);
                viewSlideList.setCurrentItem(2);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private void eventNavBottom() {
        navigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_lessions:
                    viewSlideList.setCurrentItem(0);
                    break;
                case R.id.menu_reports:
                    viewSlideList.setCurrentItem(1);
                    break;
                case R.id.menu_students:
                    viewSlideList.setCurrentItem(2);
                    break;
            }
            return false;
        });
    }

    private void initViewPager() {
        classListAdapter = new ClassListAdapter(this, getIntent().getExtras());
        viewSlideList.setAdapter(classListAdapter);
        viewSlideList.setUserInputEnabled(false);
        viewSlideList.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                switch (position) {
                    case 1: navigationView.getMenu().findItem(R.id.menu_reports).setChecked(true);
                        break;
                    case 2: navigationView.getMenu().findItem(R.id.menu_students).setChecked(true);
                        break;
                    default: navigationView.getMenu().findItem(R.id.menu_lessions).setChecked(true);
                        break;
                }
            }
        });
    }

    private void initWidgets() {
        navigationView = findViewById(R.id.nav_bottom_view);
        viewSlideList = findViewById(R.id.view_slide_list);
        acbListClass = findViewById(R.id.acb_list_class);
    }
}