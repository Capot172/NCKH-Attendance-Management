package com.atechclass.attendance.model;

import android.graphics.Bitmap;
import android.net.Uri;

public class StudentSubClassList {
    private Uri pathDataAttendanceFace;
    int present;
    private String day;
    private String time;
    private String serial;

    public StudentSubClassList(int present, String serial, String time, String day, Uri pathDataAttendanceFace) {
        this.present =present;
        this.day = day;
        this.time = time;
        this.serial = serial;
        this.pathDataAttendanceFace = pathDataAttendanceFace;
    }

    public Uri getAvatar() {
        return pathDataAttendanceFace;
    }

    public String getDay() {
        return day;
    }

    public String getTime() {
        return time;
    }

    public String getSerial() {
        return serial;
    }

    public void setAvatar(Uri pathDataAttendanceFace) {
        this.pathDataAttendanceFace = pathDataAttendanceFace;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public int getPresent() {
        return present;
    }

    public void setPresent(int present) {
        this.present = present;
    }
}
