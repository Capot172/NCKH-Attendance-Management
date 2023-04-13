package com.atechclass.attendance.function.class_list.Student;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atechclass.attendance.Language;
import com.atechclass.attendance.R;
import com.atechclass.attendance.adapter.StudentImportAdapter;
import com.atechclass.attendance.databinding.ActivityImportStudentsBinding;
import com.atechclass.attendance.interfaces.IOnCheckBox;
import com.atechclass.attendance.model.StudentModel;
import com.atechclass.attendance.ultis.UserLogin;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportStudents extends AppCompatActivity implements IOnCheckBox {
    private static final String SUBJECTS_PATH = "Subjects";
    private static final String STUDENTS_PATH = "Students";
    private static final String USER_PATH = "User";

    ActivityImportStudentsBinding importStudentsBinding;
    StudentImportAdapter adapter;
    LinearLayoutManager manager = new LinearLayoutManager(this);
    ArrayList<List<String>> listRow = new ArrayList<>();
    List<StudentModel> listStudents = new ArrayList<>();
    List<StudentModel> duplicateId = new ArrayList<>();
    List<StudentModel> invalidEmail = new ArrayList<>();
    List<StudentModel> invalidId = new ArrayList<>();
    List<StudentModel> invalidName = new ArrayList<>();
    List<String> oldID = new ArrayList<>();
    List<String> listLessons = new ArrayList<>();
    int duplicateChecked = 0;

    boolean typeCol = true;
    int numChecked = 0;
    private final Map<String, Boolean> listCheckStudents = new HashMap<>();
    private final Map<String, Boolean> listCheckDuplicateId = new HashMap<>();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser user;
    String idClass;
    Language language;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        language = new Language(this);
        language.Language();

        setContentView(R.layout.activity_import_students);
        importStudentsBinding = DataBindingUtil.setContentView(this, R.layout.activity_import_students);

        mAuth = UserLogin.getAuth();
        db = FirebaseFirestore.getInstance();

        setUpToolBar();
        dataAnalysisClipBoard();
        setDefaultListStudents();
        setCheckBosSelectAll();
    }

    @Override
    protected void onStart() {
        super.onStart();
        user = mAuth.getCurrentUser();
        if (user != null)
            setConfirmButton();
    }

    private void setUpToolBar() {
        Toolbar toolbar = importStudentsBinding.tbImportStudents;
        toolbar.setNavigationIcon(R.drawable.ic_back);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void dataAnalysisClipBoard() {
        //Get maxCol
        String[] data = getData().split("\n");
        int maxCol = 0;
        for (String str : data) {
            if (str.trim().length() != 0) {
                String[] split2 = str.split("\t");
                if (split2.length > maxCol) {
                    maxCol = split2.length;
                }
            }
        }
        // Add data to list row
        // If data length < maxCol, add empty string until data length = maxCol
        for (String str : data) {
            if (str.trim().length() != 0) {
                List<String> split2 = new ArrayList<>(Arrays.asList(str.split("\t")));
                while (split2.size() < maxCol) {
                    split2.add("");
                }
                listRow.add(split2);
            }
        }

        loadStudent(listRow, maxCol, setCol(maxCol));
    }

    private void setDefaultListStudents() {
        importStudentsBinding.txtSumChecked.setText(String.valueOf(listRow.size()));
        importStudentsBinding.txtNumChecked.setText(String.valueOf(listRow.size()));
        numChecked = listRow.size();
        importStudentsBinding.cbSelectAll.setChecked(true);
        //Set map
        for (StudentModel studentModel : listStudents) {
            listCheckStudents.put(studentModel.getId(), importStudentsBinding.cbSelectAll.isChecked());
        }
        //Update recyclerView
        updateRecyclerView(listStudents, typeCol);
    }

    /**
     * When confirm button is clicked:
     * 1: Create a new list (temp) from listStudents
     * 2: Remove unchecked students
     * 3: Check if temp is empty
     * 4: Check if id is valid
     * 5: Check if name is valid
     * 6: Check if email is valid
     * 7: Check if id is valid
     * 8: Create a new list to hold duplicated students (Compare with current student list on database)
     * 9: If there are duplicated students:
     * a) Show dialog of duplicated students
     * b) User check who they want to import
     * c) User click apply -> Remove unchecked students -> Import all students from temp
     * d) User click cancel -> return
     * 10: Else -> Import all students from temp
     **/

    private void setConfirmButton() {
        importStudentsBinding.btnConfirmImport.setOnClickListener(view -> {

            //Create temp student list
            List<StudentModel> tempList = new ArrayList<>(listStudents);

            //Remove student that are not checked
            tempList.removeIf(studentModel -> Boolean.FALSE.equals(listCheckStudents.get(studentModel.getId())));

            //Check empty checked list
            if (tempList.size() == 0) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.noCheckedStudents)
                        .setMessage(R.string.noCheckedStudentsTxt)
                        .setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss()).show();
                return;
            }


            //Check invalid ID, name and email format
            //Check duplicate ID
            invalidName.clear();
            invalidId.clear();
            invalidEmail.clear();
            duplicateId.clear();
            for (StudentModel studentModel : tempList) {
                if (studentModel.getId().isEmpty()) {
                    invalidId.add(studentModel);
                }
                if (typeCol) {
                    if (studentModel.getName().isEmpty()) {
                        invalidName.add(studentModel);
                    }
                } else {
                    if (studentModel.getName().isEmpty() && studentModel.getLastName().isEmpty()) {
                        invalidName.add(studentModel);
                    }
                }
                if (studentModel.getEmail() != null && !studentModel.getEmail().isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(studentModel.getEmail()).matches()) {
                    invalidEmail.add(studentModel);
                }

                if(oldID.contains(studentModel.getId())) {
                    duplicateId.add(studentModel);
                    duplicateChecked++;
                }
            }

            if (invalidId.size() > 0) {
                showDialogInvalid(invalidId, 1);
                return;
            }
            if (invalidName.size() > 0) {
                showDialogInvalid(invalidName, 2);
                return;
            }
            if (invalidEmail.size() > 0) {
                showDialogInvalid(invalidEmail, 3);
                return;
            }

            //Put values to check list duplicate
            for (StudentModel studentModel : duplicateId) {
                listCheckDuplicateId.put(studentModel.getId(), true);
            }

            //If there are duplicated values
            if (duplicateId.size() > 0) {
                showDialogDuplicateId(tempList);
            } else {
                importStudents(tempList);
            }

        });
    }

    private void showDialogInvalid(List<StudentModel> invalidList, int i) {
        //Invalid Name dialog
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();

        dialog.setContentView(R.layout.dialog_error_import);
        RecyclerView rcvItemDuplicate = dialog.findViewById(R.id.rcv_item);
        CheckBox cbSelectAll = dialog.findViewById(R.id.cb_select_all);
        LinearLayout layoutButtonId = dialog.findViewById(R.id.layout_button_id);
        LinearLayout layoutButtonOk = dialog.findViewById(R.id.layout_button_ok);
        TextView tvError = dialog.findViewById(R.id.tv_error);
        Button btnOk = dialog.findViewById(R.id.btn_ok);

        //Initialize dialog recyclerView
        StudentImportAdapter duplicateAdapter = new StudentImportAdapter(invalidList, i);
        duplicateAdapter.setTypeCol(typeCol);
        LinearLayoutManager manager1 = new LinearLayoutManager(this);
        rcvItemDuplicate.setLayoutManager(manager1);
        rcvItemDuplicate.setAdapter(duplicateAdapter);

        cbSelectAll.setVisibility(View.GONE);
        layoutButtonId.setVisibility(View.GONE);
        layoutButtonOk.setVisibility(View.VISIBLE);
        switch (i) {
            case 1:
                tvError.setText(R.string.invalidID);
                break;
            case 2:
                tvError.setText(R.string.invalidName);
                break;
            default:
                tvError.setText(R.string.invalidEmail);
        }

        btnOk.setOnClickListener(view1 -> dialog.dismiss());
    }

    private void showDialogDuplicateId(List<StudentModel> tempList) {
        //Initialize duplicate dialog
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();

        dialog.setContentView(R.layout.dialog_error_import);
        RecyclerView rcvItem = dialog.findViewById(R.id.rcv_item);
        CheckBox cbSelectAll = dialog.findViewById(R.id.cb_select_all);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnApply = dialog.findViewById(R.id.btn_apply);

        //Interface checkbox in dialog recyclerView
        IOnCheckBox iOnCheckBox = (checked, id) -> {
            if (!checked) {
                cbSelectAll.setChecked(false);
                duplicateChecked--;
            } else {
                duplicateChecked++;
                if (duplicateChecked == duplicateId.size())
                    cbSelectAll.setChecked(true);
            }
            listCheckDuplicateId.put(id, checked);
        };

        //Initialize dialog recyclerView
        StudentImportAdapter duplicateAdapter = new StudentImportAdapter(duplicateId, iOnCheckBox, listCheckStudents, 4);
        duplicateAdapter.setTypeCol(typeCol);
        LinearLayoutManager manager1 = new LinearLayoutManager(this);
        rcvItem.setLayoutManager(manager1);
        rcvItem.setAdapter(duplicateAdapter);

        //Default check all
        cbSelectAll.setChecked(true);

        cbSelectAll.setOnClickListener(view1 -> {
            for (StudentModel studentModel : tempList) {
                listCheckDuplicateId.put(studentModel.getId(), cbSelectAll.isChecked());
            }
            StudentImportAdapter temp = new StudentImportAdapter(duplicateId, iOnCheckBox, listCheckDuplicateId, 4);
            temp.setTypeCol(typeCol);
            rcvItem.setAdapter(temp);
        });

        btnCancel.setOnClickListener(view1 -> dialog.dismiss());

        btnApply.setOnClickListener(view1 -> {
            //Remove students that are not checked
            tempList.removeIf(studentModel -> Boolean.FALSE.equals(listCheckDuplicateId.get(studentModel.getId())));
            for (StudentModel model : tempList) {
                StorageReference gsReference = FirebaseStorage.getInstance().getReference(user.getUid() + "/" + SUBJECTS_PATH + "/" +
                        idClass + "/" + model.getId());

                gsReference.listAll().addOnSuccessListener(listResult -> {
                    for(StorageReference student : listResult.getItems()) {
                        student.delete();
                    }
                    for(StorageReference student : listResult.getPrefixes()) {
                        student.listAll().addOnSuccessListener(listResult1 -> {
                            for (StorageReference imageData : listResult1.getItems())
                                imageData.delete();
                        });
                    }
                });
                gsReference.delete();
            }
            importStudents(tempList);
            dialog.dismiss();
        });
    }


    private void importStudents(List<StudentModel> importList) {
        for (StudentModel studentModel : importList) {
            Map<String, Integer> map = new HashMap<>();
            for (String lesson : listLessons) {
                map.put(lesson, 0);
            }
            studentModel.setLessonMap(map);
            db.collection(USER_PATH).document(user.getUid()).collection(SUBJECTS_PATH).document(idClass)
                    .collection(STUDENTS_PATH).document(studentModel.getId()).set(studentModel);
        }
        setResult(RESULT_OK);
        finish();
    }

    /**
     * When clicked:
     * 1: Update number of students checked
     * 2: Update listCheckStudents map according to cbSelectAll state
     * 3: Update recyclerView with new map
     **/
    private void setCheckBosSelectAll() {
        importStudentsBinding.cbSelectAll.setOnClickListener(view -> {
            //Set number
            if (importStudentsBinding.cbSelectAll.isChecked())
                numChecked = listRow.size();
            else
                numChecked = 0;
            importStudentsBinding.txtNumChecked.setText(String.valueOf(numChecked));
            //Set map
            for (StudentModel studentModel : listStudents) {
                listCheckStudents.put(studentModel.getId(), importStudentsBinding.cbSelectAll.isChecked());
            }
            //Update recyclerView
            updateRecyclerView(listStudents, typeCol);
        });
    }

    /**
     * This function does the followings:
     * 1: Create an array of string contains columns name (Cột 1, Cột 2, ...)
     * 2: Set up default drop down to the spinners
     * 3: Set up textWatcher to the spinners
     *
     * @param maxCol: max number of columns, get from clipboard
     * @return an array of string contains columns name (Cột 1, Cột 2, ...)
     */

    private String[] setCol(int maxCol) {
        String[] strCol = new String[maxCol + 1];
        strCol[0] = getString(R.string.noneCol);
        for (int cell = 1; cell < strCol.length; cell++) {
            strCol[cell] = getString(R.string.col) + cell;
        }

        if (importStudentsBinding.rdbOneCol.isChecked()) {
            if (maxCol > 0)
                importStudentsBinding.actvMssvCol.setText(strCol[1]);
            if (maxCol > 1)
                importStudentsBinding.actvFullNameCol.setText(strCol[2]);
            if (maxCol > 2) {
                importStudentsBinding.actvPhoneCol.setText(strCol[3]);
                importStudentsBinding.actvEmailCol.setText(strCol[0]);
            }
            if (maxCol > 3) {
                importStudentsBinding.actvEmailCol.setText(strCol[4]);
            }
        }
        defaultDropDownCol(strCol);

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //Nothing
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //Nothing
            }

            @Override
            public void afterTextChanged(Editable editable) {
                loadDataCol(strCol);
            }
        };

        importStudentsBinding.actvFullNameCol.addTextChangedListener(textWatcher);
        importStudentsBinding.actvFirstNameCol.addTextChangedListener(textWatcher);
        importStudentsBinding.actvLastNameCol.addTextChangedListener(textWatcher);
        importStudentsBinding.actvPhoneCol.addTextChangedListener(textWatcher);
        importStudentsBinding.actvEmailCol.addTextChangedListener(textWatcher);
        importStudentsBinding.actvMssvCol.addTextChangedListener(textWatcher);
        return strCol;
    }

    private void defaultDropDownCol(String[] strCol) {
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, strCol);
        importStudentsBinding.actvFullNameCol.setAdapter(arrayAdapter);
        importStudentsBinding.actvFirstNameCol.setAdapter(arrayAdapter);
        importStudentsBinding.actvLastNameCol.setAdapter(arrayAdapter);
        importStudentsBinding.actvPhoneCol.setAdapter(arrayAdapter);
        importStudentsBinding.actvEmailCol.setAdapter(arrayAdapter);
        importStudentsBinding.actvMssvCol.setAdapter(arrayAdapter);
    }

    /**
     * When a column is changed, update student list to match data from new column selection.
     *
     * @param strCol: Number of columns, according to input data from clipboard.
     */
    private void loadDataCol(String[] strCol) {
        int id = 0;
        int name = 0;
        int lastName = 0;
        int phone = 0;
        int email = 0;
        for (int i = 0; i < strCol.length; i++) {
            if (strCol[i].equals(importStudentsBinding.actvMssvCol.getText().toString()))
                id = i;
            if (typeCol) {
                if (strCol[i].equals(importStudentsBinding.actvFullNameCol.getText().toString()))
                    name = i;
            } else {
                if (strCol[i].equals(importStudentsBinding.actvFirstNameCol.getText().toString()))
                    name = i;
            }
            if (strCol[i].equals(importStudentsBinding.actvLastNameCol.getText().toString()))
                lastName = i;
            if (strCol[i].equals(importStudentsBinding.actvPhoneCol.getText().toString()))
                phone = i;
            if (strCol[i].equals(importStudentsBinding.actvEmailCol.getText().toString()))
                email = i;
        }

        listStudents.clear();
        for (List<String> listCell : listRow) {
            StudentModel studentModel = new StudentModel();
            studentModel.setId((id - 1) < 0 ? "" : listCell.get(id - 1).trim());
            studentModel.setName((name - 1) < 0 ? "" : listCell.get(name - 1).trim());
            if ((lastName - 1) >= 0)
                studentModel.setLastName(listCell.get(lastName - 1).trim());
            studentModel.setPhoneNumber((phone - 1) < 0 ? "" : listCell.get(phone - 1).trim());
            studentModel.setEmail((email - 1) < 0 ? "" : listCell.get(email - 1).trim());
            listStudents.add(studentModel);
        }
        updateRecyclerView(listStudents, typeCol);
    }


    /**
     * Update recyclerView
     *
     * @param listStudents: A list of student models
     * @param typeCol:      Name: true. First name last name: false
     */
    private void updateRecyclerView(List<StudentModel> listStudents, boolean typeCol) {
        adapter = new StudentImportAdapter(listStudents, this, listCheckStudents, 0);
        adapter.setTypeCol(typeCol);
        importStudentsBinding.rcvStudentsImport.setLayoutManager(manager);
        importStudentsBinding.rcvStudentsImport.setAdapter(adapter);
    }


    private void loadStudent(List<List<String>> listRow, int maxCol, String[] strCol) {
        if (maxCol == 1) {
            importStudentsBinding.lnContainerCol.setVisibility(View.GONE);
            importStudentsBinding.containerListStudentsImport.setVisibility(View.GONE);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.notify)
                    .setMessage(R.string.invalidCol)
                    .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                        finish();
                        dialogInterface.dismiss();
                    }).show();
        } else if (maxCol == 2) {
            importStudentsBinding.txtFullnameCb.setVisibility(View.GONE);
            importStudentsBinding.rdgContainerFullName.setVisibility(View.GONE);
            importStudentsBinding.txtFirtNameTitle.setVisibility(View.GONE);
            importStudentsBinding.tilFirstNameLayout.setVisibility(View.GONE);
            importStudentsBinding.txtLastNameTitle.setVisibility(View.GONE);
            importStudentsBinding.tilLastNameLayout.setVisibility(View.GONE);
            importStudentsBinding.txtPhoneTitle.setVisibility(View.GONE);
            importStudentsBinding.tilPhoneLayout.setVisibility(View.GONE);
            importStudentsBinding.txtMailTitle.setVisibility(View.GONE);
            importStudentsBinding.tilEmailLayout.setVisibility(View.GONE);
            for (int i = 0; i < listRow.size(); i++) {
                StudentModel studentModel = new StudentModel();
                List<String> strArr2 = listRow.get(i);
                if (strArr2.size() == 2) {
                    studentModel.setId(strArr2.get(0).trim());
                    studentModel.setName(strArr2.get(1).trim());
                    studentModel.setEmail("");
                    studentModel.setPhoneNumber("");
                } else
                    studentModel.setId(strArr2.get(0).trim());
                listStudents.add(studentModel);
            }
            for (StudentModel studentModel : listStudents) {
                listCheckStudents.put(studentModel.getId(), true);
            }
            updateRecyclerView(listStudents, true);
            return;
        } else if (maxCol > 2) {
            if (importStudentsBinding.rdbOneCol.isChecked()) {
                importStudentsBinding.txtFirtNameTitle.setVisibility(View.GONE);
                importStudentsBinding.tilFirstNameLayout.setVisibility(View.GONE);
                importStudentsBinding.txtLastNameTitle.setVisibility(View.GONE);
                importStudentsBinding.tilLastNameLayout.setVisibility(View.GONE);
                showStudents(listStudents, listRow);
            }
            importStudentsBinding.rdgContainerFullName.setOnCheckedChangeListener((radioGroup, i) -> {
                if (i == R.id.rdb_one_col) {
                    importStudentsBinding.txtFirtNameTitle.setVisibility(View.GONE);
                    importStudentsBinding.tilFirstNameLayout.setVisibility(View.GONE);
                    importStudentsBinding.txtLastNameTitle.setVisibility(View.GONE);
                    importStudentsBinding.tilLastNameLayout.setVisibility(View.GONE);
                    importStudentsBinding.txtFullNameTitle.setVisibility(View.VISIBLE);
                    importStudentsBinding.tilFullNameLayout.setVisibility(View.VISIBLE);
                    importStudentsBinding.txtPhoneTitle.setVisibility(View.VISIBLE);
                    importStudentsBinding.tilPhoneLayout.setVisibility(View.VISIBLE);
                    importStudentsBinding.txtMailTitle.setVisibility(View.VISIBLE);
                    importStudentsBinding.tilEmailLayout.setVisibility(View.VISIBLE);
                    typeCol = true;
                    showStudents(listStudents, listRow);
                }
                if (i == R.id.rdb_two_col) {
                    importStudentsBinding.txtFirtNameTitle.setVisibility(View.VISIBLE);
                    importStudentsBinding.tilFirstNameLayout.setVisibility(View.VISIBLE);
                    importStudentsBinding.txtLastNameTitle.setVisibility(View.VISIBLE);
                    importStudentsBinding.tilLastNameLayout.setVisibility(View.VISIBLE);
                    importStudentsBinding.txtFullNameTitle.setVisibility(View.GONE);
                    importStudentsBinding.tilFullNameLayout.setVisibility(View.GONE);
                    hideWhenCol3(maxCol);
                    typeCol = false;
                    showStudents2Col(listStudents, listRow);
                }
                setDefaultCol(typeCol, maxCol, strCol);
            });
        }
    }

    private void showStudents2Col(List<StudentModel> list, List<List<String>> listRow) {
        list.clear();
        for (List<String> cellList : listRow) {
            StudentModel studentModel = new StudentModel();
            if (!cellList.isEmpty())
                studentModel.setId(cellList.get(0).trim());
            if (cellList.size() > 1)
                studentModel.setName(cellList.get(1).trim());
            if (cellList.size() > 2)
                studentModel.setLastName(cellList.get(2).trim());
            if (cellList.size() > 3)
                studentModel.setPhoneNumber(cellList.get(3).trim());
            if (cellList.size() > 4)
                studentModel.setEmail(cellList.get(4).trim());
            list.add(studentModel);
        }
        updateRecyclerView(list, false);
    }

    private void showStudents(List<StudentModel> list, List<List<String>> listRow) {
        list.clear();
        for (List<String> cellList : listRow) {
            StudentModel studentModel = new StudentModel();
            if (!cellList.isEmpty())
                studentModel.setId(cellList.get(0).trim());
            if (cellList.size() > 1)
                studentModel.setName(cellList.get(1).trim());
            if (cellList.size() > 2) {
                studentModel.setPhoneNumber(cellList.get(2).trim());
                studentModel.setEmail("");
            }
            if (cellList.size() > 3)
                studentModel.setEmail(cellList.get(3).trim());
            list.add(studentModel);
        }
        for (StudentModel studentModel : listStudents) {
            listCheckStudents.put(studentModel.getId(), true);
        }
        updateRecyclerView(list, true);
    }

    private void hideWhenCol3(int maxCol) {
        if (maxCol == 3) {
            importStudentsBinding.txtPhoneTitle.setVisibility(View.GONE);
            importStudentsBinding.tilPhoneLayout.setVisibility(View.GONE);
            importStudentsBinding.txtMailTitle.setVisibility(View.GONE);
            importStudentsBinding.tilEmailLayout.setVisibility(View.GONE);
        }
    }

    private void setDefaultCol(boolean b, int maxCol, String[] strCol) {
        if (b) {
            if (maxCol > 0)
                importStudentsBinding.actvMssvCol.setText(strCol[1]);
            if (maxCol > 1)
                importStudentsBinding.actvFullNameCol.setText(strCol[2]);
            if (maxCol > 2) {
                importStudentsBinding.actvPhoneCol.setText(strCol[3]);
                importStudentsBinding.actvEmailCol.setText(strCol[0]);
            }
            if (maxCol > 3) {
                importStudentsBinding.actvEmailCol.setText(strCol[4]);
            }
        } else {
            if (maxCol > 2) {
                importStudentsBinding.actvMssvCol.setText(strCol[1]);
                importStudentsBinding.actvFirstNameCol.setText(strCol[3]);
                importStudentsBinding.actvLastNameCol.setText(strCol[2]);
                importStudentsBinding.actvPhoneCol.setText(strCol[0]);
                importStudentsBinding.actvEmailCol.setText(strCol[0]);
            }
            if (maxCol > 3) {
                importStudentsBinding.actvPhoneCol.setText(strCol[4]);
                importStudentsBinding.actvEmailCol.setText(strCol[0]);
            }
            if (maxCol > 4) {
                importStudentsBinding.actvEmailCol.setText(strCol[5]);
            }
        }
        defaultDropDownCol(strCol);
    }

    private String getData() {
        Intent intent = getIntent();
        String data = intent.getStringExtra("data");
        Bundle bundle = intent.getExtras();
        oldID = bundle.getStringArrayList("studentId");
        idClass = intent.getStringExtra("idClass");
        listLessons = bundle.getStringArrayList("idLessons");
        return data;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onUnCheck(boolean checked, String id) {
        if (!checked) {
            importStudentsBinding.cbSelectAll.setChecked(false);
            numChecked--;
        }

        if (checked) {
            numChecked++;
            if (numChecked == listRow.size())
                importStudentsBinding.cbSelectAll.setChecked(true);
        }
        listCheckStudents.put(id, checked);
        importStudentsBinding.txtNumChecked.setText(String.valueOf(numChecked));
    }
}