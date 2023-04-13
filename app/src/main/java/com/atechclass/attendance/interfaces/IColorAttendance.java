package com.atechclass.attendance.interfaces;

import com.atechclass.attendance.adapter.StudentAttendanceAdapter;

public interface IColorAttendance {
    void setColor(int state, StudentAttendanceAdapter.StudentAttendanceViewHolder holder);
}
