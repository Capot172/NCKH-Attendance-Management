package com.atechclass.attendance.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.atechclass.attendance.R;
import com.atechclass.attendance.interfaces.IOnClickAddStudents;
import com.atechclass.attendance.model.FaceStudent;

import java.util.List;

public class ListFaceStudentsAdapter extends BaseAdapter {
    Activity context;
    List<FaceStudent> listFaces;
    IOnClickAddStudents clickAddStudents;

    public ListFaceStudentsAdapter(Activity context, List<FaceStudent> listFaces, IOnClickAddStudents clickAddStudents) {
        this.context = context;
        this.listFaces = listFaces;
        this.clickAddStudents = clickAddStudents;
    }

    public void updateList(List<FaceStudent> listFaces) {
        this.listFaces = listFaces;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return listFaces.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    class ViewHolder {
        ImageView imgFaceStudent;
        TextView idStudent;
        TextView nameStudent;
        TextView addStudent;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if(view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_face_student, null);
            holder = new ViewHolder();
            holder.imgFaceStudent = view.findViewById(R.id.img_photo_detect);
            holder.idStudent = view.findViewById(R.id.txt_id_student_item);
            holder.nameStudent = view.findViewById(R.id.txt_name_student_item);
            holder.addStudent = view.findViewById(R.id.btn_add_student_photo);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        if(listFaces.get(i).getiDFace().equals("") || listFaces.get(i).getNameFace().equals("")) {
            holder.idStudent.setVisibility(View.GONE);
            holder.nameStudent.setVisibility(View.GONE);
            holder.addStudent.setVisibility(View.VISIBLE);
            holder.addStudent.setOnClickListener(view1 -> {
                clickAddStudents.addStudents(i, listFaces.get(i).getImgFace(), listFaces.get(i).getEmbs());
            });
        } else {
            holder.idStudent.setVisibility(View.VISIBLE);
            holder.nameStudent.setVisibility(View.VISIBLE);
            holder.idStudent.setText(listFaces.get(i).getiDFace());
            holder.nameStudent.setText(listFaces.get(i).getNameFace());
            holder.addStudent.setVisibility(View.GONE);
        }

        holder.imgFaceStudent.setImageBitmap(listFaces.get(i).getImgFace());
        return view;
    }

}
