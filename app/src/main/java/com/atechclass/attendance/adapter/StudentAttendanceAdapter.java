package com.atechclass.attendance.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atechclass.attendance.R;
import com.atechclass.attendance.interfaces.IColorAttendance;
import com.atechclass.attendance.interfaces.IOnclickStudentATD;
import com.atechclass.attendance.model.StudentModel;

import java.util.List;

public class StudentAttendanceAdapter extends RecyclerView.Adapter<StudentAttendanceAdapter.StudentAttendanceViewHolder>{
    private List<StudentModel> students;
    IOnclickStudentATD iOnclickStudentATD;
    IColorAttendance iColorAttendance;
    private int flag;
    String lessonID;

    public StudentAttendanceAdapter(List<StudentModel> students, IOnclickStudentATD iOnclickStudentATD,String lessonID, IColorAttendance iColorAttendance) {
        this.students = students;
        this.iOnclickStudentATD = iOnclickStudentATD;
        this.lessonID = lessonID;
        this.iColorAttendance = iColorAttendance;
        setHasStableIds(true);
    }

    public void updateList(List<StudentModel> mList) {
        this.students = mList;
        notifyDataSetChanged();
    }

    public void updateStatus(int flag) {
        this.flag = flag;
    }

    @NonNull
    @Override
    public StudentAttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_attendance, parent, false);
        return new StudentAttendanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentAttendanceViewHolder holder, int position) {
        StudentModel attendance = students.get(position);
        String fullName;
        if (attendance.getLastName() != null){
            fullName = attendance.getLastName() + " " + attendance.getName();
        } else {
            fullName = attendance.getName();
        }
        holder.name.setText(fullName);

        String serial = String.valueOf(position+1);
        if (position < 9){
            serial = "0" + serial;
        }
        holder.serial.setText(serial);
        String idText = " " + attendance.getId();
        holder.id.setText(idText);
        holder.check.setOnClickListener(view -> iOnclickStudentATD.updateStatus(flag, attendance, holder));
        //Set image
        int state = attendance.getLessonMap().getOrDefault(lessonID, 0);
        iColorAttendance.setColor(state, holder);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return students != null ? students.size() : 0;
    }


    public static class StudentAttendanceViewHolder extends RecyclerView.ViewHolder {
        private final TextView serial;
        private final TextView name;
        private final TextView id;
        public ImageView check;

        public StudentAttendanceViewHolder(@NonNull View itemView) {
            super(itemView);
            serial = itemView.findViewById(R.id.serial);
            name = itemView.findViewById(R.id.txt_name_students);
            check = itemView.findViewById(R.id.img_check);
            id = itemView.findViewById(R.id.txt_id_students);
        }
    }
}
