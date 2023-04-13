package com.atechclass.attendance.interfaces;

import android.net.Uri;

import com.atechclass.attendance.adapter.StudentRecyclerViewAdapter;
import com.atechclass.attendance.model.StudentModel;

public interface IOnClickStudent {
    void onClick(StudentModel studentSubject);
    void onClickPopMenu(StudentRecyclerViewAdapter.StudentViewHolder holder, StudentModel studentModel, int position, Uri avatarImage);
}
