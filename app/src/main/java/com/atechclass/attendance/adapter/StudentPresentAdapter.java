package com.atechclass.attendance.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atechclass.attendance.R;
import com.atechclass.attendance.model.StudentModel;

import java.util.List;

public class StudentPresentAdapter extends RecyclerView.Adapter<StudentPresentAdapter.StudentsPresentViewHolder> {
    List<StudentModel> list;
    Context context;

    public StudentPresentAdapter(List<StudentModel> list, Context context) {
        this.list = list;
        this.context = context;
    }

    public void setList(List<StudentModel> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StudentsPresentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_student_present, parent, false);
        return new StudentsPresentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentsPresentViewHolder holder, int position) {
        StudentModel studentModel = list.get(position);

        holder.txtId.setText(studentModel.getId());
        holder.txtName.setText(studentModel.getName());
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public class StudentsPresentViewHolder extends RecyclerView.ViewHolder {
        TextView txtName;
        TextView txtId;
        public StudentsPresentViewHolder(@NonNull View itemView) {
            super(itemView);
            txtId = itemView.findViewById(R.id.txt_id_student_present);
            txtName = itemView.findViewById(R.id.txt_name_student_present);
        }
    }
}
