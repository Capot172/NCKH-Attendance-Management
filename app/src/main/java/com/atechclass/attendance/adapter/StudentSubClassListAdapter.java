package com.atechclass.attendance.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atechclass.attendance.R;
import com.atechclass.attendance.interfaces.IOnClickLessonStudentInfo;
import com.atechclass.attendance.model.StudentSubClassList;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class StudentSubClassListAdapter extends RecyclerView.Adapter<StudentSubClassListAdapter.StudentInfoViewHolder> {
    private List<StudentSubClassList> studentInfoClasses;
    private IOnClickLessonStudentInfo onClick;
    public StudentSubClassListAdapter(List<StudentSubClassList> studentInfoClasses, IOnClickLessonStudentInfo clickItemLessons) {
        this.studentInfoClasses = studentInfoClasses;
        this.onClick = clickItemLessons;
    }

    public void updateList(List<StudentSubClassList> uList){
        this.studentInfoClasses = uList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StudentInfoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lessons_student_info, parent, false);
        return new StudentInfoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentInfoViewHolder holder, int position) {
        StudentSubClassList studentInfo = studentInfoClasses.get(position);
        holder.day.setText(studentInfo.getDay());
        holder.serial.setText(studentInfo.getSerial());
        holder.time.setText(studentInfo.getTime());
        holder.parent.setOnClickListener(view -> onClick.onClick(position));

        if(studentInfo.getAvatar() != null)
            Picasso.get().load(studentInfo.getAvatar()).into(holder.faceData);

        GradientDrawable drawable = (GradientDrawable) holder.dot.getBackground();
            switch (studentInfo.getPresent()) {
            case 0:
                drawable.setColor(Color.parseColor("#F27373"));
                break;
            case 1:
                drawable.setColor(Color.parseColor("#53C781"));
                break;
            case 2:
                drawable.setColor(Color.parseColor("#F8FC2A"));
                break;
        }
    }


    @Override
    public int getItemCount() {
        return studentInfoClasses != null ? studentInfoClasses.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public static class StudentInfoViewHolder extends RecyclerView.ViewHolder{
        private final TextView serial;
        private final TextView day;
        private final TextView time;
        ImageView dot;
        ImageButton edit;
        LinearLayout parent;
        CircleImageView faceData;

        public StudentInfoViewHolder(@NonNull View view){
            super(view);
            serial = view.findViewById(R.id.txt_seri);
            day = view.findViewById(R.id.txt_day);
            time = view.findViewById(R.id.txt_time);
            dot = view.findViewById(R.id.img_dot);
            edit = view.findViewById(R.id.edtBtn);
            parent = view.findViewById(R.id.parent);
            faceData = view.findViewById(R.id.img_data_attendance);
        }
    }
}
