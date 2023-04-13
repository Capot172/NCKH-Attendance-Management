package com.atechclass.attendance.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atechclass.attendance.R;
import com.atechclass.attendance.interfaces.IOnCheckBox;
import com.atechclass.attendance.model.StudentModel;

import java.util.List;
import java.util.Map;

public class StudentImportAdapter extends RecyclerView.Adapter<StudentImportAdapter.ClassViewHolder>{
    private final List<StudentModel> studentModels;
    private boolean typeCol;
    private IOnCheckBox checkBox;
    private Map<String, Boolean> listCheckStudents;
    private final int type;

    public StudentImportAdapter(List<StudentModel> studentModels, IOnCheckBox checkBox, Map<String, Boolean> listCheckStudents, int type) {
        this.studentModels = studentModels;
        this.checkBox = checkBox;
        this.listCheckStudents = listCheckStudents;
        this.type = type;
    }

    public StudentImportAdapter(List<StudentModel> studentModels, int type) {
        this.studentModels = studentModels;
        this.type = type;
    }

    public void setTypeCol(boolean typeCol) {
        this.typeCol = typeCol;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_import, parent, false);
        return new ClassViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        StudentModel studentModel = studentModels.get(position);

        if(typeCol) {
            setUpInfo(holder.txtNameStudentImport, studentModel.getName() != null ? studentModel.getName() : "");
        } else {
            String name;
            if (studentModel.getLastName() != null){
                name = studentModel.getLastName() + " " + studentModel.getName();
            } else {
                name = studentModel.getName();
            }
            setUpInfo(holder.txtNameStudentImport, name);
        }
        setUpInfo(holder.txtIDStudentImport, studentModel.getId() != null ? studentModel.getId() : "");
        setUpInfo(holder.txtPhoneStudentImport, studentModel.getPhoneNumber() != null ? studentModel.getPhoneNumber() : "");
        setUpInfo(holder.txtEmailStudentImport, studentModel.getEmail() != null ? studentModel.getEmail() : "");

        switch (type) {
            case 1:
                holder.cbStudentImport.setVisibility(View.GONE);
                holder.tvID.setTextColor(Color.RED);
                break;
            case 2:
                holder.cbStudentImport.setVisibility(View.GONE);
                holder.tvName.setTextColor(Color.RED);
                break;
            case 3:
                holder.cbStudentImport.setVisibility(View.GONE);
                holder.tvEmail.setTextColor(Color.RED);
                break;
            case 4:
                holder.tvID.setTextColor(Color.RED);
                holder.cbStudentImport.setChecked(Boolean.TRUE.equals(listCheckStudents.get(studentModel.getId())));

                holder.cbStudentImport.setOnClickListener(view -> checkBox.onUnCheck(holder.cbStudentImport.isChecked(), studentModel.getId()));
                break;
            default:
                holder.cbStudentImport.setChecked(Boolean.TRUE.equals(listCheckStudents.get(studentModel.getId())));

                holder.cbStudentImport.setOnClickListener(view -> checkBox.onUnCheck(holder.cbStudentImport.isChecked(), studentModel.getId()));
        }
    }

    private void setUpInfo(TextView txtInfo, String data) {
        txtInfo.setText(data);
    }

    @Override
    public int getItemCount() {
        return studentModels != null ? studentModels.size() : 0;
    }


    public static class ClassViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtNameStudentImport;
        private final TextView txtIDStudentImport;
        private final TextView txtPhoneStudentImport;
        private final TextView txtEmailStudentImport;
        private final CheckBox cbStudentImport;
        private final TextView tvID;
        private final TextView tvName;
        private final TextView tvEmail;

        public ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNameStudentImport = itemView.findViewById(R.id.txt_name_student_import);
            txtIDStudentImport = itemView.findViewById(R.id.txt_id_student_import);
            txtPhoneStudentImport = itemView.findViewById(R.id.txt_phone_student_import);
            txtEmailStudentImport = itemView.findViewById(R.id.txt_email_student_import);
            cbStudentImport = itemView.findViewById(R.id.cb_student_import);
            tvID = itemView.findViewById(R.id.tv_id);
            tvName = itemView.findViewById(R.id.tv_name);
            tvEmail = itemView.findViewById(R.id.tv_email);
        }
    }
}
