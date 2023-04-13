package com.atechclass.attendance.interfaces;

import com.atechclass.attendance.adapter.StudentAttendanceAdapter;
import com.atechclass.attendance.model.StudentModel;

public interface IOnclickStudentATD {
    void updateStatus(Integer listStatus, StudentModel studentModel, StudentAttendanceAdapter.StudentAttendanceViewHolder holder);
}
