package com.atechclass.attendance.function.class_list;

import static com.atechclass.attendance.ultis.CalendarUtils.getEndMonth;
import static com.atechclass.attendance.ultis.CalendarUtils.getFirstDay;
import static com.atechclass.attendance.ultis.CalendarUtils.localDate;
import static com.atechclass.attendance.ultis.CalendarUtils.monDay;
import static com.atechclass.attendance.ultis.CalendarUtils.monthYearFromDate;
import static com.atechclass.attendance.ultis.CalendarUtils.sunDay;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.atechclass.attendance.Language;
import com.atechclass.attendance.R;
import com.atechclass.attendance.model.ClassModel;
import com.atechclass.attendance.model.LessonModel;
import com.atechclass.attendance.model.StudentModel;
import com.atechclass.attendance.model.StudentsExcel;
import com.atechclass.attendance.ultis.CalendarUtils;
import com.atechclass.attendance.ultis.CharAnalysis;
import com.atechclass.attendance.ultis.PieCharAnalysis;
import com.atechclass.attendance.ultis.UserLogin;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Reports extends Fragment {
    private static final int COL_INDEX_STT = 0;
    private static final int COL_INDEX_ID = 1;
    private static final int COL_INDEX_FIRST_NAME = 2;
    private static final int COL_INDEX_LAST_NAME = 3;
    private static final int COL_INDEX_PHONE = 4;
    private static final int COL_INDEX_EMAIL = 5;
    private static final int COL_DEFAULT = 6;
    
    private static final String SUBJECTS_PATH = "Subjects";
    private static final String STUDENTS_PATH = "Students";
    private static final String USER_PATH = "User";
    private static final String LESSON_PATH = "Lesson";
    private static final String ALLOW_KEY = "ALLOW_KEY";

    private static final int COL_ABSENT = 1;
    private static final int COL_LATE = 2;
    private static final int COL_PERCENT_PRESENT = 3;
    private static final int LENGTH_CELL = 416;
    private static final int ROW_SUM = 1;

    private static final int[] COLORS = new int[]{
            R.color.pastel_green,
            R.color.pastel_red,
            R.color.pastel_yellow
    };

    private PieChart pStatistical;
    private PieChart pStatisticalWeek;
    private PieChart pStatisticalMonth;
    private PieChart pStatisticalSemester;
    private BarChart bStatistical;
    private BarChart bStatisticalSemester;
    private BarChart bStatisticalMonth;

    private final List<StudentModel> listStudentModel = new ArrayList<>();
    private final List<LessonModel> listLessonIDModel = new ArrayList<>();
    private final List<LessonModel> list = new ArrayList<>();
    private final List<StudentModel> studentList = new ArrayList<>();

    private String[] dayOfWeek ;
    private LinearLayout ctnWeek;
    private LinearLayout ctnMonth;
    private LinearLayout ctnSemester;

    private RadioGroup btnReports;
    private RadioButton btnWeek;
    private TextView dayWeekYear;
    private TextView weekYear;
    private TextView txtMonthOfYear;
    private TextView txtViewProgressMonth;
    private TextView txtViewProgressAll;
    private ImageView nextWeek;
    private ImageView nextMonth;
    private ImageView prevWeek;
    private ImageView preMonth;
    private ImageButton btnExportFile;
    private TextView txtLessonNumber;
    private TextView txtLessonHaveLearned;
    private TextView txtLessonRemain;
    private TextView viewProgress;

    private RelativeLayout viewLoad;
    private RelativeLayout viewReportEmpty;
    private LinearLayout viewReportNoEmpty;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private ClassModel classRoot;
    private Language language;

    private int maxON = 0;
    private int maxId = 0;
    private int maxFirstName = 0;
    private int maxLastName = 0;
    private int maxEmail = 0;
    private int maxPhone = 0;
    private int lessonHaveLearned;

    private LocalDate startWeek;
    private LocalDate endWeek;
    private LocalDate boundaryStartWeeks;
    private LocalDate boundaryEndWeeks;
    private String dayOfWeeks;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        language = new Language(getActivity());
        language.Language();
        View view = inflater.inflate(R.layout.fragment_reports, container, false);
        initWidgets(view);
        dayOfWeek = new String[] {getString(R.string.mon), getString(R.string.tue), getString(R.string.wed), getString(R.string.thu), getString(R.string.fri), getString(R.string.sat), getString(R.string.sun)};

        mAuth = UserLogin.getAuth();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            classRoot = (ClassModel) getArguments().getSerializable("class");
        }

        requireActivity().getSupportFragmentManager().setFragmentResultListener("attendance", this, (requestKey, bundle) -> {
            boolean result = bundle.getBoolean("hasChanged");
            if (result){
                getDB();
            }
        });

        changeView();
        requestPermission();


        return view;
    }



    @Override
    public void onResume() {
        super.onResume();
        getDataStudentFromDataBase();
    }


    //Cấp quyền storage
    private void requestPermission() {
        btnExportFile.setOnClickListener(view1 -> {
            if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                                    != PackageManager.PERMISSION_GRANTED) {
                if(Boolean.TRUE.equals(getFromPerf(getContext()))) {
                    //Access setting
                    showSetting();
                } else if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
                }
            } else {
                openExcel();
            }
        });
    }

    //Check trạng thái permission
    private static Boolean getFromPerf(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(ALLOW_KEY, Context.MODE_PRIVATE);
        return preferences.getBoolean(ALLOW_KEY, false);
    }

    //Lưu trạng thái permisssion
    private static void saveToPreferences(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(ALLOW_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(ALLOW_KEY, true);
        editor.apply();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 101) {
            //Ghi file nếu cấp quyền thành công
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openExcel();
            } else if(grantResults[0] == PackageManager.PERMISSION_DENIED)
            //Chuyển trạng thái truy cập vào setting khi từ chối
            {
                boolean showRotinable = ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if(!showRotinable) {
                    saveToPreferences(getActivity());
                }
            }
        }
    }

    //Get data from firebase store
    private void getDataStudentFromDataBase() {
        //Get object students
        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH)
                .document(classRoot.getId() + "").collection(STUDENTS_PATH)
                .get().addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        listStudentModel.clear();
                        for(QueryDocumentSnapshot snapshot : task.getResult()) {
                            StudentModel object = snapshot.toObject(StudentModel.class);
                            listStudentModel.add(object);
                        }
                    }
                });

        //Get object lessons
        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH)
                .document(classRoot.getId() + "").collection(LESSON_PATH)
                .get().addOnCompleteListener(task -> {
                    listLessonIDModel.clear();
                    if(task.isSuccessful()) {
                        listLessonIDModel.clear();
                        for(QueryDocumentSnapshot snapshot : task.getResult()) {
                            LessonModel lessonModel = snapshot.toObject(LessonModel.class);
                            listLessonIDModel.add(lessonModel);
                        }
                    }
                });
    }

    private void openExcel() {
        if(listStudentModel.size() != 0 && listLessonIDModel.size() != 0) {
            try {
                writeExcel();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Warning!!!")
                    .setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_warning, null))
                    .setMessage("No data to export file")
                    .setNegativeButton("OK", null).create().show();
        }
    }

    private void writeExcel() throws IOException {
        //Định dang đuôi file excel .xlsx
        XSSFWorkbook  workbook = new XSSFWorkbook();
        //Create sheet name in excel
        XSSFSheet sheet = workbook.createSheet("Sheet 1");

        //Khởi tạo đối tượng student excel
        List<StudentsExcel> excelList = new ArrayList<>();
        //Chuyển đổi trạng thái của học sinh int -> String
        for(StudentModel student : listStudentModel) {
            List<String> lessonsStatus = new ArrayList<>();
            for (LessonModel lessonModel : listLessonIDModel){
                int status = student.getLessonMap().get(String.valueOf(lessonModel.getId())) == null ? 0 : student.getLessonMap().get(String.valueOf(lessonModel.getId()));
                lessonsStatus.add(getStatus(status));
            }
            //Thêm thông tin hs vào đối tượng excelList
            excelList.add(new StudentsExcel(student.getId(), student.getName(), student.getLastName(),
                    student.getEmail(), student.getPhoneNumber(), lessonsStatus));
        }

        //Khởi tạo header cho excel
        createHeader(sheet, excelList.get(0).getStatus().size());

        //Lặp số lượng học
        for(int i = 0 ; i < excelList.size(); i++) {
            XSSFRow row = sheet.createRow(i+1);
            XSSFCell cell;
            //Các trường mặc định: id, name, email, phone
            for(int l = 0; l < COL_DEFAULT; l++) {
                cell = row.createCell(l);
                CellStyle cellStyle = getCellStyleBorder(sheet);
                cell.setCellStyle(cellStyle);
                if(l == COL_INDEX_STT){
                    maxON = setValueCell(sheet, maxON, cell, i+1+"", COL_INDEX_STT);
                }else if(l == COL_INDEX_ID) {
                    maxId = setValueCell(sheet, maxId, cell, excelList.get(i).getId(), COL_INDEX_ID);
                }
                else if(l == COL_INDEX_FIRST_NAME) {
                    maxFirstName = setValueCell(sheet, maxFirstName, cell, excelList.get(i).getLastName() == null ? "" : excelList.get(i).getLastName(), COL_INDEX_FIRST_NAME);
                }else if(l == COL_INDEX_LAST_NAME) {
                    maxLastName = setValueCell(sheet, maxLastName, cell, excelList.get(i).getName(), COL_INDEX_LAST_NAME);
                } else if(l == COL_INDEX_EMAIL) {
                    maxEmail = setValueCell(sheet, maxEmail, cell, excelList.get(i).getEmail(), COL_INDEX_EMAIL);
                } else {
                    cell.setCellValue(excelList.get(i).getPhoneNumber());
                    if(String.valueOf(excelList.get(i).getPhoneNumber()).length() * (256+64) > maxPhone) {
                        sheet.setColumnWidth(COL_INDEX_PHONE, String.valueOf(excelList.get(i).getPhoneNumber()).length() * (256+64));
                        maxPhone = String.valueOf(excelList.get(i).getPhoneNumber()).length() * (256 + 64);
                    }
                }
            }
            String cellEndLesson = null;
            String cellStrartLesson = new CellAddress(i+1, COL_DEFAULT).toString();
            //Tạo n buổi học và điền giá trị
            for(int j = 0; j < excelList.get(i).getStatus().size(); j++) {
                CellStyle cellStyle = getCellStyleCenter(sheet);
                cell = row.createCell(j + COL_DEFAULT);
                cell.setCellStyle(cellStyle);
                cell.setCellValue(excelList.get(i).getStatus().get(j).trim());
                cellEndLesson = new CellAddress(i+1, j + COL_DEFAULT).toString();
            }

            int sizeAll = excelList.get(i).getStatus().size() + COL_DEFAULT;
//            //Thống kê trạng thái có mặt, vắng mặt,trễ
            int COL_DEFAULT_STATUS = 3;
            for(int j = sizeAll; j <= (sizeAll + COL_DEFAULT_STATUS); j++) {
                CellStyle cellStyle = getCellStyleCenter(sheet);
                cell = row.createCell(j);
                cell.setCellStyle(cellStyle);
                int COL_PRESENT = 0;
                if((sizeAll + COL_PRESENT) == j)
                    cell.setCellFormula("COUNTIF(" + cellStrartLesson + ":" + cellEndLesson + ", \"" + getString(R.string.txt_present) + "\")");
                else if((sizeAll + COL_ABSENT) == j)
                    cell.setCellFormula("COUNTIF(" + cellStrartLesson + ":" + cellEndLesson + ", \"" + getString(R.string.txt_absent) + "\")");
                else if((sizeAll + COL_LATE) == j)
                    cell.setCellFormula("COUNTIF(" + cellStrartLesson + ":" + cellEndLesson + ", \"" + getString(R.string.txt_late) + "\")");
                else if((sizeAll + COL_PERCENT_PRESENT) == j)
                    cell.setCellFormula("COUNTIF(" + cellStrartLesson + ":" + cellEndLesson + ", \"" + getString(R.string.txt_present) + "\")/COUNTA(" + cellStrartLesson + ":" + cellEndLesson + ") * 100");
            }

        }

        sheet.addMergedRegion(new CellRangeAddress(excelList.size()+ ROW_SUM, excelList.size()+ ROW_SUM, COL_INDEX_STT, COL_INDEX_EMAIL));
        CellStyle cellStyle = createStyleForHeader(sheet);
        XSSFRow rowSum = sheet.createRow(excelList.size() + ROW_SUM);
        XSSFCell cellSum = rowSum.createCell(0);
        cellSum.setCellValue(getString(R.string.txt_sum));
        cellSum.setCellStyle(cellStyle);
        CellStyle cellStyleSum = getCellStyleCenter(sheet);

        for(int i = 0; i < excelList.get(0).getStatus().size(); i++) {
            String cellStart = new CellAddress(1, COL_DEFAULT+i).toString();
            String cellEnd = null;
            for(int j = 0 ; j < excelList.size(); j ++) {
                cellEnd = new  CellAddress(j+1, COL_DEFAULT+i).toString();
            }
            cellSum = rowSum.createCell(i + COL_DEFAULT);
            cellSum.setCellStyle(cellStyleSum);
            cellSum.setCellFormula("COUNTIF(" + cellStart + ":" + cellEnd + ", \"" + getString(R.string.txt_present) + "\")");
        }


        for(int i = excelList.get(0).getStatus().size(); i < (excelList.get(0).getStatus().size() + 4); i++) {
            String cellStart = new  CellAddress(1, COL_DEFAULT+i).toString();
            String cellEnd = null;
            int sum = 0;
            for(int j = 0 ; j < excelList.size(); j ++) {
                cellEnd = new  CellAddress(j+1, COL_DEFAULT+i).toString();
                sum++;
            }
            cellSum = rowSum.createCell(i + COL_DEFAULT);
            cellSum.setCellStyle(cellStyleSum);
            if(i != (excelList.get(0).getStatus().size() + 4)-1)
                cellSum.setCellFormula("SUM(" + cellStart + ":" + cellEnd + ")/(" + (listStudentModel.get(0).getLessonMap().size() * listStudentModel.size()) + ") *100 & \"%\"");
            else
                cellSum.setCellFormula("SUM(" + cellStart + ":" + cellEnd + ")/(" + sum + ")");
        }

        //Mở excel
        openExcel(workbook);
    }

    private int setValueCell(XSSFSheet sheet, int maxId, XSSFCell cell, String id, int colIndexId) {
        cell.setCellValue(id);
        if (id.length() * LENGTH_CELL > maxId) {
            sheet.setColumnWidth(colIndexId, id.length() * LENGTH_CELL);
            maxId = id.length() * LENGTH_CELL;
        }
        return maxId;
    }

    private void openExcel(XSSFWorkbook workbook) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
        //path
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath(),
                "Attendance_" + format.format(new Date()) + ".xlsx");
        try (FileOutputStream os = new FileOutputStream(file)) {
            //Viết file vào excel
            workbook.write(os);
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Check file exist
        if(file.exists()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                //get type mime extension
                String mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(file.getName()));
                //Cho phép mở file offline
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(FileProvider.getUriForFile(getContext(),
                        getActivity().getResources().getString(R.string.path_author), file), mimeTypeFromExtension);
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), getString(R.string.not_open_file), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), getString(R.string.file_not_exist), Toast.LENGTH_LONG).show();
        }
    }

    @NonNull
    private CellStyle getCellStyleCenter(XSSFSheet sheet) {
        CellStyle cellStyle = sheet.getWorkbook().createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        return cellStyle;
    }

    @NonNull
    private CellStyle getCellStyleBorder(XSSFSheet sheet) {
        CellStyle cellStyle = sheet.getWorkbook().createCellStyle();
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        return cellStyle;
    }

    private String getStatus(int status) {
        switch (status) {
            case 0:
                return getString(R.string.txt_absent);
            case 1:
                return getString(R.string.txt_present);
            default:
                return getString(R.string.txt_late);
        }
    }

    private void createHeader(XSSFSheet sheet, int excelList) {
        CellStyle cellStyle = createStyleForHeader(sheet);

        XSSFRow row = sheet.createRow(0);

        XSSFCell cell = row.createCell(COL_INDEX_STT);
        cell.setCellStyle(cellStyle);
        cell.setCellValue(getString(R.string.txt_stt));
        maxON = setValueCell(sheet, maxON, cell, getString(R.string.txt_stt), COL_INDEX_STT);

        cell = row.createCell(COL_INDEX_ID);
        cell.setCellStyle(cellStyle);
        cell.setCellValue("ID");
        maxId = setValueCell(sheet, maxId, cell, "ID", COL_INDEX_ID);


        cell = row.createCell(COL_INDEX_FIRST_NAME);
        cell.setCellStyle(cellStyle);
        cell.setCellValue(getString(R.string.txt_first_name));
        maxFirstName = setValueCell(sheet, maxFirstName, cell, getString(R.string.txt_first_name), COL_INDEX_FIRST_NAME);

        cell = row.createCell(COL_INDEX_LAST_NAME);
        cell.setCellStyle(cellStyle);
        cell.setCellValue(getString(R.string.txt_last_name));
        maxFirstName = setValueCell(sheet, maxFirstName, cell, getString(R.string.txt_last_name), COL_INDEX_LAST_NAME);

        cell = row.createCell(COL_INDEX_PHONE);
        cell.setCellStyle(cellStyle);
        cell.setCellValue(getString(R.string.txt_phone));
        maxPhone = setValueCell(sheet, maxPhone, cell, getString(R.string.txt_phone), COL_INDEX_PHONE);

        cell = row.createCell(COL_INDEX_EMAIL);
        cell.setCellStyle(cellStyle);
        cell.setCellValue("Email");
        maxEmail = setValueCell(sheet, maxEmail, cell, "Email", COL_INDEX_EMAIL);

        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        for(int i = 0; i < excelList; i++) {
            DateFormat formatTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date timeStart = new Date(listLessonIDModel.get(i).getStartTime());
            Date timeEnd =  new Date(listLessonIDModel.get(i).getEndTime());
            String date = dateFormat.format(listLessonIDModel.get(i).getDayTime());
            cell = row.createCell(i+COL_DEFAULT);
            cell.setCellStyle(cellStyle);
            cell.setCellValue(getString(R.string.lesson) + (i+1) + "\n" + date + "\n" + formatTime.format(timeStart) + " - " + formatTime.format(timeEnd));
            sheet.setColumnWidth(i+COL_DEFAULT, (listLessonIDModel.get(i).getStartTime() + " - " + listLessonIDModel.get(i).getEndTime()).length() * (256 + 128));
        }

        cell = row.createCell(excelList + 6);
        cell.setCellStyle(cellStyle);
        cell.setCellValue(getString(R.string.txt_present));

        cell = row.createCell(excelList + 7);
        cell.setCellStyle(cellStyle);
        cell.setCellValue(getString(R.string.txt_absent));

        cell = row.createCell(excelList + 8);
        cell.setCellStyle(cellStyle);
        cell.setCellValue(getString(R.string.txt_late));

        cell = row.createCell(excelList + 9);
        cell.setCellStyle(cellStyle);
        cell.setCellValue(getString(R.string.txt_percent_present));
        sheet.setColumnWidth(excelList + 9, getString(R.string.txt_percent_present).length() * (256+64));
    }

    private CellStyle createStyleForHeader(XSSFSheet sheet) {
        Font font = sheet.getWorkbook().createFont();
        font.setFontName("Times New Roman");
        font.setBold(true);

        XSSFCellStyle cellStyle = sheet.getWorkbook().createCellStyle();
        cellStyle.setFont(font);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        return cellStyle;
    }

    private void showSetting() {
        Intent accessSetting = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
        accessSetting.setData(uri);
        startActivityForResult(accessSetting, 103);
    }

    private void changeView() {
        btnWeek.setChecked(true);
        btnReports.setOnCheckedChangeListener((radioGroup, i) -> {
            if (i == R.id.btn_weeks){
                ctnWeek.setVisibility(View.VISIBLE);
                ctnMonth.setVisibility(View.GONE);
                ctnSemester.setVisibility(View.GONE);
            }
            if (i == R.id.btn_months){
                ctnWeek.setVisibility(View.GONE);
                ctnMonth.setVisibility(View.VISIBLE);
                ctnSemester.setVisibility(View.GONE);
            }
            if (i == R.id.btn_semesters){
                ctnWeek.setVisibility(View.GONE);
                ctnMonth.setVisibility(View.GONE);
                ctnSemester.setVisibility(View.VISIBLE);
            }
        });
    }
    @Override
    public void onStart() {
        getDB();
        super.onStart();
    }

    private void getDB() {
        //Get object students
        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId()+"").collection(STUDENTS_PATH)
                .get().addOnCompleteListener(task -> {
                    studentList.clear();
                    for(QueryDocumentSnapshot studentQuery : task.getResult()){
                        StudentModel studentModel = studentQuery.toObject(StudentModel.class);
                        studentList.add(studentModel);
                    }
                });
        // Get object lesson
        db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(classRoot.getId() + "").collection(LESSON_PATH)
                .get().addOnCompleteListener(task -> {
                    list.clear();
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document2 : task.getResult()) {
                            LessonModel lessonModel = document2.toObject(LessonModel.class);
                            list.add(lessonModel);
                        }
                        if(list.size() != 0) {
                            viewReportEmpty.setVisibility(View.GONE);
                            viewReportNoEmpty.setVisibility(View.VISIBLE);
                        } else {
                            viewReportEmpty.setVisibility(View.VISIBLE);
                            viewReportNoEmpty.setVisibility(View.GONE);
                        }

                        viewLoad.setVisibility(View.GONE);
                        statisticalLesson();
                        initBarCharWeek();
                        initBarCharAll();
                        getDayTime();
                    }
                });
    }



    private void initBarCharWeek() {
        if (list.size() != 0) {
            boundary();

            LocalDate date = LocalDate.now();
            if(date.isBefore(boundaryStartWeeks) || boundaryEndWeeks.isBefore(date))
                date = Instant.ofEpochMilli(list.get(0).getDayTime()).atZone(ZoneId.systemDefault()).toLocalDate();

            startWeek = monDay(date);
            setWeek();

            nextWeek.setOnClickListener(view -> {
                if(endWeek.isBefore(boundaryEndWeeks)) {
                    startWeek = startWeek.plusWeeks(1);
                    setWeek();
                }
            });

            prevWeek.setOnClickListener(view -> {
                if(boundaryStartWeeks.isBefore(startWeek)) {
                    startWeek = startWeek.minusWeeks(1);
                    setWeek();
                }
            });
        }
    }

    private void setWeek() {
        endWeek = sunDay(startWeek);
        dayOfWeeks = startWeek.getDayOfMonth() + "/" + startWeek.getMonthValue() + "/" + startWeek.getYear() + " - "
                + endWeek.getDayOfMonth() + "/" + endWeek.getMonthValue() + "/" + endWeek.getYear();
        dayWeekYear.setText(dayOfWeeks);
        TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
        int weekNumber = startWeek.get(woy);
        weekYear.setText(getString(R.string.week) + " " + weekNumber);
        calculateDayOfWeek(startWeek, endWeek);
    }

    private void boundary() {
        boundaryEndWeeks = Instant.ofEpochMilli(list.get(list.size() - 1).getDayTime()).atZone(ZoneId.systemDefault()).toLocalDate();
        boundaryEndWeeks = sunDay(boundaryEndWeeks);
        boundaryStartWeeks = Instant.ofEpochMilli(list.get(0).getDayTime()).atZone(ZoneId.systemDefault()).toLocalDate();
        boundaryStartWeeks = monDay(boundaryStartWeeks);
    }

    private void calculateDayOfWeek(LocalDate startWeek, LocalDate endWeek) {
        LocalDate today = LocalDate.now();
        List<BarEntry> barEntries = new ArrayList<>();
        List<Integer> textColors = new ArrayList<>();
        float i = 0f;
        int presentWeekPie = 0;
        int absentWeekPie = 0;
        int lateWeekPie = 0;
        int sumWeekPie = 0;
        while (!startWeek.isAfter(endWeek)) {
            int presentWeek = 0;
            int absentWeek = 0;
            int lateWeek = 0;
            int sumWeek = 0;
            for(LessonModel lessonModel : list) {
                LocalDate dayLesson = Instant.ofEpochMilli(lessonModel.getDayTime()).atZone(ZoneId.systemDefault()).toLocalDate();
                if(dayLesson.equals(startWeek)){
                    presentWeek += lessonModel.getPresent();
                    absentWeek += lessonModel.getAbsent();
                    lateWeek += lessonModel.getLate();
                    presentWeekPie += lessonModel.getPresent();
                    absentWeekPie += lessonModel.getAbsent();
                    lateWeekPie += lessonModel.getLate();
                }
            }

            if(startWeek.equals(today))
                textColors.add(getResources().getColor(R.color.pastel_red));
            else
                textColors.add(getResources().getColor(R.color.black));
            sumWeek = presentWeek + absentWeek + lateWeek;
            float[] values = new float [] {percentFloat(presentWeek, sumWeek), percentFloat(absentWeek, sumWeek), percentFloat(lateWeek, sumWeek)};
            barEntries.add(new BarEntry(0.5f + i, values));
            i++;
            startWeek = startWeek.plusDays(1);
        }

        BarDataSet barDataSetWeek = new BarDataSet(barEntries, "");
        barDataSetWeek.setColors(COLORS, getContext());
        barDataSetWeek.setValueTextColors(textColors);

        bStatistical.setXAxisRenderer(new CharAnalysis.ColoredLabelAxisRenderer(bStatistical.getViewPortHandler(), bStatistical.getXAxis(), bStatistical.getTransformer(YAxis.AxisDependency.LEFT), textColors));
        BarData barData = new BarData(barDataSetWeek);
        barData.setBarWidth(.6f);
        bStatistical.setData(barData);

        CharAnalysis charAnalysis = new CharAnalysis(getContext(), bStatistical);
        charAnalysis.setUpAxis(dayOfWeek);
        charAnalysis.hideLine(true);

        //pie week
        sumWeekPie = presentWeekPie + absentWeekPie + lateWeekPie;
        int[] all = {percentInt(presentWeekPie, sumWeekPie), percentInt(absentWeekPie, sumWeekPie), percentInt(lateWeekPie, sumWeekPie)};
        if(sumWeekPie != 0)
            viewProgress.setText(percentString(presentWeekPie, lateWeekPie, sumWeekPie) + "%");
        else
            viewProgress.setText("0%");
        PieCharAnalysis.initPieStatic(getActivity(), all);
        PieCharAnalysis.getStatistical(pStatisticalWeek);
        PieCharAnalysis.setCirleWidth(pStatisticalWeek, 80, true);
    }
    LocalDate endMonth;
    LocalDate startMonth;
    private void getDayTime(){
        // Ngày bắt đầu và ngày kết thúc một buổi học
        if(list.size() == 0){
            txtMonthOfYear.setText(monthYearFromDate(localDate));
        } else {
            endMonth = Instant.ofEpochMilli(list.get(list.size() - 1).getDayTime()).atZone(ZoneId.systemDefault()).toLocalDate();
            startMonth = Instant.ofEpochMilli(list.get(0).getDayTime()).atZone(ZoneId.systemDefault()).toLocalDate();

            endMonth = getEndMonth(endMonth);
            startMonth = getFirstDay(startMonth);

            initChartMonth();
            txtMonthOfYear.setText(monthYearFromDate(localDate));

            nextMonth.setOnClickListener(view -> {
                if (localDate.plusMonths(1).isBefore(endMonth)){
                    localDate = localDate.plusMonths(1);
                    txtMonthOfYear.setText(monthYearFromDate(localDate));
                    initChartMonth();
                }
            });
            preMonth.setOnClickListener(view -> {
                if (localDate.minusMonths(1).isAfter(startMonth)) {
                    localDate = localDate.minusMonths(1);
                    txtMonthOfYear.setText(monthYearFromDate(localDate));
                    initChartMonth();
                }
            });
        }

    }

    // Xử lí thống kê tất cả
    private void initBarCharAll() {
        CharAnalysis charAnalysis = new CharAnalysis(getContext(), bStatisticalSemester);
        ArrayList<BarEntry> barEntriesSemesterPresent = new ArrayList<>();
        ArrayList<BarEntry> barEntriesSemesterAbsent = new ArrayList<>();
        ArrayList<BarEntry> barEntriesSemesterLate = new ArrayList<>();

        //Grid chart
        List<String> listMoth = new ArrayList<>();
        if(list.size() != 0) {
            Calendar cldStart = Calendar.getInstance();
            Calendar cldEnd = Calendar.getInstance();
            cldStart.setTimeInMillis(list.get(0).getDayTime());
            cldEnd.setTimeInMillis(list.get(list.size() - 1).getDayTime());
            listMoth = new ArrayList<>(CalendarUtils.getListMonth(cldStart, cldEnd));
            for(int i = 0 ; i < listMoth.size(); i++) {
                int presentMonth = 0;
                int absentMonth = 0;
                int lateMonth = 0;
                int sumMonth = 0;
                Calendar calendar = Calendar.getInstance();
                for(LessonModel lessonModel : list) {
                    calendar.setTimeInMillis(lessonModel.getDayTime());
                    if(listMoth.get(i).equals((calendar.get(Calendar.MONTH) + 1) + "/" + calendar.get(Calendar.YEAR))) {
                        presentMonth += lessonModel.getPresent();
                        absentMonth += lessonModel.getAbsent();
                        lateMonth += lessonModel.getLate();
                    }
                }
                sumMonth = presentMonth + absentMonth + lateMonth;
                barEntriesSemesterPresent.add(new BarEntry(calendar.get(Calendar.MONTH) + 1f, percentFloat(presentMonth, sumMonth)));
                barEntriesSemesterAbsent.add(new BarEntry(calendar.get(Calendar.MONTH) + 1f, percentFloat(absentMonth, sumMonth)));
                barEntriesSemesterLate.add(new BarEntry(calendar.get(Calendar.MONTH) + 1f, percentFloat(lateMonth, sumMonth)));

            }
        }

        charAnalysis.initBarChart2C(barEntriesSemesterPresent, barEntriesSemesterAbsent, barEntriesSemesterLate);
        charAnalysis.setUpAxis2C(listMoth, listMoth.size());
        charAnalysis.hideLine(false);

        //Pie chart
        int present = 0;
        int absent = 0;
        int late = 0;
        int sum = 0;

        for(LessonModel lessonModel : list) {
            if(lessonModel.getFlag() == 1) {
                present += lessonModel.getPresent();
                absent += lessonModel.getAbsent();
                late += lessonModel.getLate();
            }
        }

        sum += present + absent + late;

        int[] all = {percentInt(present, sum), percentInt(absent, sum), percentInt(late, sum)};
        if(sum != 0)
            txtViewProgressAll.setText(percentString(present, late, sum) + "%");
        PieCharAnalysis.initPieStatic(getActivity(), all);
        PieCharAnalysis.getStatistical(pStatisticalSemester);
        PieCharAnalysis.setCirleWidth(pStatisticalSemester, 80, true);
        bStatisticalSemester.getAxisLeft().setDrawGridLines(false);
    }

    private int percentInt(int percent, int sum) {
        return (int) ((float) (percent*100) / sum);
    }

    private float percentFloat(int percent, int sum) {
        if(sum == 0)
            return 0;
        return (((percent * 1.0f ) * 100.0f) / (sum*1.0f));
    }

    private String percentString(int percent, int late, int sum) {
        float calculation = (((percent+late) * 100f) / sum);
        DecimalFormat format = new DecimalFormat("0.##");
        return (calculation % 1 == 0 ? String.valueOf((int) calculation) : format.format(calculation));
    }

    private void initChartMonth() {
        CharAnalysis charAnalysis = new CharAnalysis(getContext(), bStatisticalMonth);

        int presentMonth = 0;
        int absentMonth =0;
        int lateMonth = 0;
        int lessonNumber = 0;
        int sum;

        List<String> listWeek;
        ArrayList<BarEntry> barEntriesWeekPresent = new ArrayList<>();
        ArrayList<BarEntry> barEntriesWeekAbsent = new ArrayList<>();
        ArrayList<BarEntry> barEntriesWeekLate = new ArrayList<>();

        if (list.size() != 0){
            Calendar cldStart = Calendar.getInstance();
            cldStart.setTimeInMillis(Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()).getTime());
            cldStart.setMinimalDaysInFirstWeek(1);
            listWeek = new ArrayList<>(CalendarUtils.getListWeek(cldStart));

            int presentWeek;
            int absentWeek;
            int lateWeek;
            int sumWeek;

            Calendar calendar = Calendar.getInstance();

            for (int i = 0; i < listWeek.size(); i ++){
                presentWeek = 0;
                absentWeek = 0;
                lateWeek = 0;
                sumWeek = 0;
                for (LessonModel lessonModel : list){
                    calendar.setTimeInMillis(lessonModel.getDayTime());
                    calendar.setMinimalDaysInFirstWeek(1);
                    if (calendar.get(Calendar.YEAR) != cldStart.get(Calendar.YEAR) || calendar.get(Calendar.MONTH) > cldStart.get(Calendar.MONTH))
                        break;
                    if (calendar.get(Calendar.MONTH) < cldStart.get(Calendar.MONTH))
                        continue;

                    int present = lessonModel.getPresent();
                    int absent = lessonModel.getAbsent();
                    int late = lessonModel.getLate();

                    if (listWeek.get(i).equals("Tuần " + calendar.get(Calendar.WEEK_OF_YEAR))
                        && (present !=0 || absent!=0 || late !=0)){

                        presentWeek += present;
                        absentWeek += absent;
                        lateWeek += late;

                        lessonNumber ++;

                        sumWeek = presentWeek + absentWeek + lateWeek;
                    }
                }
                if (sumWeek != 0){
                    barEntriesWeekPresent.add(new BarEntry(i + 1f, percentFloat(presentWeek, sumWeek)));
                    barEntriesWeekAbsent.add(new BarEntry(i + 1f, percentFloat(absentWeek, sumWeek)));
                    barEntriesWeekLate.add(new BarEntry(i + 1f, percentFloat(lateWeek, sumWeek)));

                    presentMonth += presentWeek;
                    absentMonth += absentWeek;
                    lateMonth += lateWeek;
                } else {
                    barEntriesWeekPresent.add(new BarEntry(i + 1f, 0));
                    barEntriesWeekAbsent.add(new BarEntry(i + 1f, 0));
                    barEntriesWeekLate.add(new BarEntry(i + 1f, 0));
                }
            }
            sum = presentMonth + absentMonth + lateMonth;

            charAnalysis.initBarChart2C(barEntriesWeekPresent, barEntriesWeekAbsent, barEntriesWeekLate);
            charAnalysis.setUpAxis2C(listWeek, listWeek.size());
            charAnalysis.hideLine(false);
            bStatisticalMonth.getAxisLeft().setDrawGridLines(false);

            //Tổng số liệu
            if(sum == 0 || list.size() == 0){
                int[] all = {0,0,0};
                txtViewProgressMonth.setText(0+"%");
                PieCharAnalysis.initPieStatic(getActivity(), all);
                PieCharAnalysis.getStatistical(pStatisticalMonth);
                PieCharAnalysis.setCirleWidth(pStatisticalMonth, 80, true);
            }
            else{
                int[] all = {(presentMonth*100/(sum*lessonNumber)), (absentMonth*100/(sum*lessonNumber)),(lateMonth*100/(sum*lessonNumber))};
                txtViewProgressMonth.setText(percentString(presentMonth, lateMonth, sum)+"%");
                PieCharAnalysis.initPieStatic(getActivity(), all);
                PieCharAnalysis.getStatistical(pStatisticalMonth);
                PieCharAnalysis.setCirleWidth(pStatisticalMonth, 80, true);
            }

        }
    }
    // Thống kê số buổi học
    private void statisticalLesson() {
        int lessonNumber = list.size();
        lessonHaveLearned = 0;
        for(LessonModel lessonModel : list){
            if(lessonModel.getAbsent() !=0 || lessonModel.getLate() != 0 || lessonModel.getPresent() != 0) lessonHaveLearned ++;
        }
        int lessonRemain = lessonNumber - lessonHaveLearned;
        txtLessonNumber.setText(" "+ lessonNumber);
        txtLessonRemain.setText(" "+ lessonRemain);
        txtLessonHaveLearned.setText(" "+lessonHaveLearned);

        int[] study = {lessonHaveLearned, lessonRemain, 0};
        PieCharAnalysis.initPieLesson(requireActivity(), study);
        PieCharAnalysis.getStatistical(pStatistical);
        PieCharAnalysis.setCirleWidth(pStatistical, 0, false);
    }


    private void initWidgets(View view) {
        btnReports = view.findViewById(R.id.group_btn_reports);
        btnWeek = view.findViewById(R.id.btn_weeks);
        ctnWeek = view.findViewById(R.id.container_week_report);
        ctnMonth = view.findViewById(R.id.container_month_reports);
        ctnSemester = view.findViewById(R.id.container_all_reports);
        pStatistical = view.findViewById(R.id.pie_statistical);
        bStatistical = view.findViewById(R.id.bar_statistical);
        bStatisticalSemester = view.findViewById(R.id.pie_statistical_semester);
        pStatisticalWeek = view.findViewById(R.id.progressBar);
        pStatisticalMonth = view.findViewById(R.id.pie_statistical_month);
        pStatisticalSemester = view.findViewById(R.id.progressBarAll);
        bStatisticalMonth = view.findViewById(R.id.line_statistical_month);
        btnExportFile = view.findViewById(R.id.btn_export);
        dayWeekYear = view.findViewById(R.id.day_week_year_tv);
        weekYear = view.findViewById(R.id.week_year_tv);
        nextWeek = view.findViewById(R.id.btn_next_week);
        prevWeek = view.findViewById(R.id.btn_prev_week);
        txtMonthOfYear = view.findViewById(R.id.txt_month_of_year);
        nextMonth = view.findViewById(R.id.btn_next_month);
        preMonth = view.findViewById(R.id.btn_prev_month);
        txtViewProgressMonth = view.findViewById(R.id.txtViewProgressMonth);
        txtViewProgressAll = view.findViewById(R.id.txtViewProgressAll);

        txtLessonNumber = view.findViewById(R.id.txt_lesson_number);
        txtLessonHaveLearned = view.findViewById(R.id.lesson_have_learned);
        txtLessonRemain = view.findViewById(R.id.txt_lesson_remain);
        viewProgress = view.findViewById(R.id.txtViewProgress);
        viewReportEmpty = view.findViewById(R.id.view_report_empty);
        viewReportNoEmpty = view.findViewById(R.id.view_report_no_empty);
        viewLoad = view.findViewById(R.id.loadingPanel);
    }

}