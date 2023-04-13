package com.atechclass.attendance;

import static com.atechclass.attendance.ultis.CalendarUtils.getToday;
import static com.atechclass.attendance.ultis.CalendarUtils.localDate;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atechclass.attendance.adapter.ClassRecyclerViewAdapter;
import com.atechclass.attendance.function.Feedback;
import com.atechclass.attendance.function.Policy;
import com.atechclass.attendance.function.QAndA.QAndA;
import com.atechclass.attendance.function.class_list.ClassListItem;
import com.atechclass.attendance.interfaces.IOnClickItemClass;
import com.atechclass.attendance.model.ClassModel;
import com.atechclass.attendance.model.LessonModel;
import com.atechclass.attendance.ultis.UserLogin;
import com.atechclass.attendance.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, IOnClickItemClass {

    private static final String LANGUAGE = "language";
    private static final String DARK_MODE = "sp_dark_mode";
    private static final String SUBJECTS_PATH = "Subjects";
    private static final String USER_PATH = "User";
    private boolean hasToday = false;
    private long backPressTime;

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private Toolbar toolbar;

    private LinearLayout classTodayLayout;
    private LinearLayout noClassLayout;
    private RecyclerView rcvItemClassList;
    private RecyclerView rcvItemClassTodayList;
    private ClassRecyclerViewAdapter classListAdapter;
    private ClassRecyclerViewAdapter classTodayListAdapter;
    private Intent changeLayout;

    private FirebaseAuth mAuth;
    private GoogleSignInClient gsc;
    private GoogleSignInOptions gso;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private Locale locale;
    private Language language;

    private final List<ClassModel> classList = new ArrayList<>();
    private final List<ClassModel> todayClassList = new ArrayList<>();
    private final List<ClassModel> allClassList = new ArrayList<>();

    private FloatingActionButton floatingActionButton;
    private Switch drawerSwitch;
    private TextView tvLanguage;
    private Dialog languageDialog;
    private AppCompatImageButton btnDeleteAll;
    private SharedPreferences spDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        spDarkMode = getSharedPreferences(DARK_MODE, Context.MODE_PRIVATE);
        if(spDarkMode.getBoolean(DARK_MODE, false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        language = new Language(this);
        language.Language();
        OMR();

        setDarkModeSwitch();

        setToolBar();

        mAuth = UserLogin.getAuth();
        db = FirebaseFirestore.getInstance();
        gsc = UserLogin.getGsc(this, getResources().getString(R.string.default_web_client_id));

        mAuth = FirebaseAuth.getInstance();
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        gsc = GoogleSignIn.getClient(this, gso);

        user = mAuth.getCurrentUser();
        if (user != null) {
            setUserInfo();
            getDataFromDatabase();
        }

        setClassListRecyclerView();

        setClassListTodayRecyclerView();

        setBtnCreateClass();

        setBtnDeleteAll();

        setLanguageDialog();
    }

    //This function is used to get class list from database
    private void getDataFromDatabase() {
        findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
        //Get today
        DateFormat format = new SimpleDateFormat(getString(R.string.fomat_dmy), Locale.getDefault());
        localDate = LocalDate.now();
        String today = getToday(localDate, getString(R.string.fomat_dmy));
        //Query to the location of a class
        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        allClassList.clear();
                        classList.clear();
                        todayClassList.clear();
                        for(QueryDocumentSnapshot classSnapshot : task.getResult()){
                            ClassModel itemClass = classSnapshot.toObject(ClassModel.class);
                            allClassList.add(itemClass);
                        }
                        for (ClassModel classModel: allClassList){
                            db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH)
                                    .document(classModel.getId()+"").collection("Lesson")
                                    .get().addOnCompleteListener(task1 -> {
                                        hasToday = false;
                                        if (!task.getResult().isEmpty()){
                                            for (QueryDocumentSnapshot lessonSnapshot: task1.getResult()){
                                                hasToday = false;
                                                LessonModel lessonModel = lessonSnapshot.toObject(LessonModel.class);
                                                String lessonDate = format.format(lessonModel.getDayTime());
                                                if (today.equals(lessonDate)){
                                                    hasToday = true;
                                                    break;
                                                }
                                            }
                                        }
                                        //Add class to list
                                        //If hasToday = true -> todayClassList, else classList
                                        addClass(classModel);
                                        classList.sort((classModel1, t1) -> classModel1.getSubject().compareToIgnoreCase(t1.getSubject()));
                                        todayClassList.sort((classModel1, t1) -> classModel1.getSubject().compareToIgnoreCase(t1.getSubject()));
                                        updateRecyclerView();
                                        updateTodayRecyclerView();
                                    });
                        }
                    }
                    //If there is no class, show no class layout
                    showNoClass(task);
                    findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                });
    }

    private void showNoClass(Task<QuerySnapshot> task) {
        if (task.getResult().isEmpty()){
            noClassLayout.setVisibility(View.VISIBLE);
            btnDeleteAll.setVisibility(View.GONE);
        } else {
            noClassLayout.setVisibility(View.GONE);
            btnDeleteAll.setVisibility(View.VISIBLE);
        }
    }

    private void addClass(ClassModel itemClass) {
        if (!hasToday){
            classList.add(itemClass);
        } else {
            todayClassList.add(itemClass);
        }
    }

    //This function is used to set the toolbar and the drawer layout
    private void setToolBar() {
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,
                drawerLayout,
                toolbar,
                R.string.nav_drawer_open,
                R.string.nav_drawer_close);

        toggle.getDrawerArrowDrawable().setColor(ResourcesCompat.getColor(getResources(), R.color.black, null));

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navView.setNavigationItemSelectedListener(this);
    }

    private void OMR() {
        noClassLayout = findViewById(R.id.noClass);
        classTodayLayout = findViewById(R.id.classToday);
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        rcvItemClassList = findViewById(R.id.rcvItemClassList);
        rcvItemClassTodayList = findViewById(R.id.rcvItemClassTodayList);
        floatingActionButton = findViewById(R.id.btn_create);
        btnDeleteAll = findViewById(R.id.btn_delete_all);
    }

    //This function is used to set the user info and avatar
    private void setUserInfo() {

        //Initialize img items
        CircleImageView imgAvatarDrawer = navView.getHeaderView(0).findViewById(R.id.profile_image);
        CircleImageView imgAvatarToolbar = toolbar.findViewById(R.id.img_toolbar_avatar);

        //Set textView with user email
        TextView tvUserEmail = navView.getHeaderView(0).findViewById(R.id.email);
        tvUserEmail.setText(user.getEmail());

        //Load the avatar to circle image view via picasso lib
        Picasso.get().load(user.getPhotoUrl())
                .placeholder(R.drawable.images).error(R.drawable.img_item_class)
                .into(imgAvatarDrawer);
        Picasso.get().load(user.getPhotoUrl())
                .placeholder(R.drawable.images).error(R.drawable.img_item_class)
                .into(imgAvatarToolbar);
    }

    private void setLanguageDialog(){
        tvLanguage = navView.getMenu().findItem(R.id.class_language).getActionView().findViewById(R.id.tv_language);

        languageDialog = new Dialog(this);
        languageDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        languageDialog.setContentView(R.layout.dialog_language);
        languageDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        languageDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        RadioGroup rg = languageDialog.findViewById(R.id.radio_group);
        Button btnCancel = languageDialog.findViewById(R.id.btn_cancel);
        Button btnApply = languageDialog.findViewById(R.id.btn_apply);

        SharedPreferences spLanguageOption = getSharedPreferences(LANGUAGE, Context.MODE_PRIVATE );
        String check = spLanguageOption.getString(LANGUAGE, "Vietnamese");

        RadioButton rdEnglish = languageDialog.findViewById(R.id.radio_english);
        RadioButton rdChinese = languageDialog.findViewById(R.id.radio_chinese);
        RadioButton rdKorean = languageDialog.findViewById(R.id.radio_korean);
        RadioButton rdVietnamese = languageDialog.findViewById(R.id.radio_vietnamese);
        RadioButton rdJapanese = languageDialog.findViewById(R.id.radio_japanese);

        switch (check) {
            case "Chinese":
                tvLanguage.setText(R.string.chinese);
                rdChinese.setChecked(true);
                break;
            case "Korean":
                tvLanguage.setText(R.string.korean);
                rdKorean.setChecked(true);
                break;
            case "Vietnamese":
                tvLanguage.setText(R.string.vietnamese);
                rdVietnamese.setChecked(true);
                break;

            case "Japanese":
                tvLanguage.setText(R.string.japanese);
                rdJapanese.setChecked(true);
                break;
            case "English":
                tvLanguage.setText(R.string.english);
                rdEnglish.setChecked(true);
                break;
        }


        btnCancel.setOnClickListener(view -> {
            rg.clearCheck();
            languageDialog.cancel();
        });
        btnApply.setOnClickListener(view -> {
            int rdBtnID = rg.getCheckedRadioButtonId();
            SharedPreferences.Editor editor = spLanguageOption.edit();
            switch (rdBtnID){
                case R.id.radio_english:
                    editor.putString(LANGUAGE,"English");
                    tvLanguage.setText(R.string.english);
                    locale = new Locale("en");
                    break;
                case R.id.radio_japanese:
                    editor.putString(LANGUAGE,"Japanese");
                    tvLanguage.setText(R.string.japanese);
                    locale = new Locale("ja");
                    break;
                case R.id.radio_chinese:
                    editor.putString(LANGUAGE,"Chinese");
                    tvLanguage.setText(R.string.chinese);
                    locale = new Locale("ii");
                    break;
                case R.id.radio_korean:
                    editor.putString(LANGUAGE,"Korean");
                    tvLanguage.setText(R.string.korean);
                    locale = new Locale("ko");
                    break;
                case R.id.radio_vietnamese:
                    editor.putString(LANGUAGE,"Vietnamese");
                    tvLanguage.setText(R.string.vietnamese);
                    locale = new Locale("vi");
                    break;
            }
            setLocale(locale);
            editor.commit();
            languageDialog.cancel();
        });
    }

    private void setLocale(Locale locale) {
        Locale.setDefault(locale);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = locale;
        res.updateConfiguration(conf, dm);
        MainActivity.this.recreate();
    }


    //This function is used to switch dark mode
    private void setDarkModeSwitch() {
        drawerSwitch = navView.getMenu().findItem(R.id.class_darkmode).getActionView().findViewById(R.id.darkMode_switch);
        drawerSwitch.setChecked(spDarkMode.getBoolean(DARK_MODE, false));
        drawerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = spDarkMode.edit();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                editor.putBoolean(DARK_MODE, true);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                editor.putBoolean(DARK_MODE, false);
            }
            editor.commit();
        });
    }

    private void setClassListRecyclerView() {
        //Initialize recyclerView and adapter
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rcvItemClassList.setLayoutManager(linearLayoutManager);
        classListAdapter = new ClassRecyclerViewAdapter(classList, this);
        updateRecyclerView();
    }
    private void setClassListTodayRecyclerView() {
        //Initialize recyclerView and adapter
        LinearLayoutManager linearLayoutManager1 = new LinearLayoutManager(this);
        rcvItemClassTodayList.setLayoutManager(linearLayoutManager1);
        classTodayListAdapter = new ClassRecyclerViewAdapter(todayClassList, this);
        updateTodayRecyclerView();
    }

    private void updateRecyclerView() {
        //Update adapter with current list
        classListAdapter.updateList(classList);
        //Set adapter for recyclerView
        rcvItemClassList.setAdapter(classListAdapter);
    }
    private void updateTodayRecyclerView() {
        //Show/hide layout
        if (todayClassList.isEmpty()){
            classTodayLayout.setVisibility(View.GONE);
        }
        else{
            classTodayLayout.setVisibility(View.VISIBLE);
        }
        //Update adapter with current list
        classTodayListAdapter.updateList(todayClassList);
        //Set adapter for recyclerView
        rcvItemClassTodayList.setAdapter(classTodayListAdapter);
    }

    private void setBtnCreateClass() {
        floatingActionButton.setOnClickListener(view -> {
            //Initialize create dialog
            Dialog createDialog = new Dialog(this);
            createDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            createDialog.setContentView(R.layout.dialog_create_class);
            createDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            createDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            Button btnCreate = createDialog.findViewById(R.id.btn_create);
            Button btnCancel = createDialog.findViewById(R.id.btn_cancel);
            EditText edtClassName = createDialog.findViewById(R.id.et_class_name);
            //Button cancel
            btnCancel.setOnClickListener(view1 -> createDialog.cancel());
            //Button create
            btnCreate.setOnClickListener(view1 -> {
                String className = edtClassName.getText().toString().trim();
                if (TextUtils.isEmpty(className)){
                    edtClassName.setError(getString(R.string.no_empty));
                    edtClassName.requestFocus();
                } else {
                    if (classList.isEmpty()){
                        noClassLayout.setVisibility(View.GONE);
                        btnDeleteAll.setVisibility(View.VISIBLE);
                    }
                    //Create class model
                    ClassModel classModel = new ClassModel(className);
                    //Upload class model to database
                    db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH)
                            .document(classModel.getId()+"").set(classModel);
                    //Add to classList
                    classList.add(classModel);
                    //Sort
                    classList.sort((classModel1, t1) -> classModel1.getSubject().compareToIgnoreCase(t1.getSubject()));
                    //Update
                    updateRecyclerView();
                    //Cancel createDialog
                    createDialog.cancel();
                }
            });
            //Show createDialog
            createDialog.show();
        });
    }

    private void setBtnDeleteAll() {
        btnDeleteAll.setOnClickListener(view -> {
            Dialog deleteAllDialog = new Dialog(this);
            deleteAllDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            deleteAllDialog.setContentView(R.layout.dialog_delete_all);
            deleteAllDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            deleteAllDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            Button btnDelete = deleteAllDialog.findViewById(R.id.btn_delete_all_class);
            Button btnCancel = deleteAllDialog.findViewById(R.id.btn_cancel);

            btnCancel.setOnClickListener(view1 -> deleteAllDialog.cancel());
            btnDelete.setOnClickListener(view1 -> {
                for (ClassModel classModel : classList){
                    db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH)
                            .document(classModel.getId()+"").delete();
                }
                classList.clear();
                for (ClassModel classModel : todayClassList){
                    db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH)
                            .document(classModel.getId()+"").delete();
                }
                todayClassList.clear();
                updateRecyclerView();
                updateTodayRecyclerView();
                noClassLayout.setVisibility(View.VISIBLE);
                deleteAllDialog.dismiss();
            });
            deleteAllDialog.show();
        });
    }

    //Drawer on item select method
    @Override
    public boolean onNavigationItemSelected( MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.class_language){
            languageDialog.show();
        }
        if (id == R.id.class_darkmode){
            drawerSwitch.setChecked(!drawerSwitch.isChecked());
        }
        if (id == R.id.class_question_answer){
            changeLayout = new Intent(MainActivity.this, QAndA.class);
            startActivity(changeLayout);
        }
        if (id == R.id.class_question_feedback){
            changeLayout = new Intent(MainActivity.this, Feedback.class);
            startActivity(changeLayout);
        }
        if (id == R.id.class_policy){
            changeLayout = new Intent(MainActivity.this, Policy.class);
            startActivity(changeLayout);
        }
        //Sign out
        if (id == R.id.class_logout){
            gsc.signOut();
            mAuth.signOut();
            Intent intent = new Intent(MainActivity.this, Login.class);
            startActivity(intent);
            finish();
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        //If drawer is open, close it
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (backPressTime + 2000 > System.currentTimeMillis()) {     //Press 2 time to exit app
            super.onBackPressed();
            return;
        }else {
            Toast.makeText(this, "Press Back Again To Exit", Toast.LENGTH_SHORT).show();
        }
        backPressTime = System.currentTimeMillis();
    }

    //On click method for class item
    //Go to ClassListItem class with data from a class
    @Override
    public void onClick(ClassModel classModel) {
        Intent intent = new Intent(this, ClassListItem.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("class", classModel);
        intent.putExtras(bundle);
        startActivityForResult(intent, 101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK){
            getDataFromDatabase();
        }
    }

    //On click method for class item's popup menu
    @Override
    public void onClickPopMenu(ClassRecyclerViewAdapter.ClassViewHolder holder, ClassModel classModel, int position) {
        //Initialize popup menu
        holder.popupMenu = new PopupMenu(this, holder.itemView.findViewById(R.id.icMenu));
        holder.popupMenu.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        holder.popupMenu.getMenuInflater().inflate(R.menu.menu_popup, holder.popupMenu.getMenu());
        MenuCompat.setGroupDividerEnabled(holder.popupMenu.getMenu(), true);
        //Pop up menu on click
        holder.popupMenu.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.menu_edit){
                //Initialize edit dialog
                Dialog editDialog = new Dialog(this);
                editDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                editDialog.setContentView(R.layout.dialog_edit_class);
                editDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                editDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                EditText etClassName = editDialog.findViewById(R.id.et_class_name);
                Button btnEdit = editDialog.findViewById(R.id.btn_edit);
                Button btnCancel = editDialog.findViewById(R.id.btn_cancel);

                //Put cursor at end of text
                etClassName.setText(classModel.getSubject());
                etClassName.requestFocus();
                etClassName.setSelection(etClassName.getText().length());

                //Edit button
                btnEdit.setOnClickListener(view -> {
                    //Check if the class name is valid
                    String newName = etClassName.getText().toString();
                    if (TextUtils.isEmpty(newName)){
                        etClassName.setError(getString(R.string.no_empty) );
                        etClassName.requestFocus();
                    } else {
                        //Edit the class with new name
                        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH)
                                .document(classModel.getId()+"").update("subject", newName);
                        if (classList.contains(classModel)){
                            classList.get(position).setSubject(newName);
                            //Sort
                            classList.sort((classModel1, t1) -> classModel1.getSubject().compareToIgnoreCase(t1.getSubject()));
                            //Update
                            updateRecyclerView();
                        } else {
                            todayClassList.get(position).setSubject(newName);
                            //Sort
                            todayClassList.sort((classModel1, t1) -> classModel1.getSubject().compareToIgnoreCase(t1.getSubject()));
                            //Update
                            updateTodayRecyclerView();
                        }
                        //Cancel editDialog
                        editDialog.cancel();
                    }
                });
                //Cancel button
                btnCancel.setOnClickListener(view -> editDialog.cancel());
                //Show editDialog
                editDialog.show();
            }
            if (id == R.id.menu_delete){
                //Initialize delete dialog
                Dialog deleteDialog = new Dialog(this);
                deleteDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                deleteDialog.setContentView(R.layout.dialog_delete_class);
                deleteDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                deleteDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                EditText etClassName = deleteDialog.findViewById(R.id.et_class_name);
                Button btnDelete = deleteDialog.findViewById(R.id.btn_delete);
                Button btnCancel = deleteDialog.findViewById(R.id.btn_cancel);
                etClassName.setText(classModel.getSubject());

                //Delete button
                btnDelete.setOnClickListener(view -> {
                    //Delete the class from database
                    db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH)
                            .document(classModel.getId()+"").delete();
                    //Remove from the list
                    if (classList.remove(classModel)) updateRecyclerView();
                    if (todayClassList.remove(classModel)) updateTodayRecyclerView();
                    //No class layout
                    if (classList.isEmpty() && todayClassList.isEmpty()){
                        noClassLayout.setVisibility(View.VISIBLE);
                        btnDeleteAll.setVisibility(View.GONE);
                    }
                    //Cancel deleteDialog
                    StorageReference ref = FirebaseStorage.getInstance().getReference(user.getUid() + "/" + SUBJECTS_PATH + "/" +
                            classModel.getId());
                    ref.listAll().addOnSuccessListener(listResult -> {
                        for (StorageReference student : listResult.getPrefixes()) {
                            student.listAll().addOnSuccessListener(listResult1 -> {
                                for (StorageReference studentImage : listResult1.getPrefixes()) {
                                    studentImage.listAll().addOnSuccessListener(listResult2 -> {
                                       for(StorageReference studentAvatar : listResult2.getItems()) {
                                           studentAvatar.delete();
                                       }
                                    });
                                }
                                for (StorageReference studentImage : listResult1.getItems()) {
                                    studentImage.delete();
                                }
                            });
                        }
                    });

                    deleteDialog.cancel();
                });
                //Cancel button
                btnCancel.setOnClickListener(view -> deleteDialog.cancel());
                //Show deleteDialog
                deleteDialog.show();
            }
            return true;
        });
        //Show popupmenu
        holder.popupMenu.show();
    }
}