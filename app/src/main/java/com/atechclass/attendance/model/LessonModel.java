package com.atechclass.attendance.model;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

public class LessonModel implements Serializable {
    long dayTime;
    long startTime;
    long endTime;
    String address;
    String room;
    int position;
    long id;
    int flag;
    Date date2 = null;
    Calendar calendar2 = Calendar.getInstance();
    int present;
    int absent;
    int late;

    public LessonModel(){}

    public LessonModel(long dayTime, long startTime, long endTime, String address, String room, int flag) throws ParseException {
        this.dayTime = dayTime;
        this.startTime = startTime;
        this.endTime = endTime;
        this.address = address;
        this.room = room;
        date2 = new Date(startTime);
        calendar2.setTime(date2);
        this.id = dayTime + (long) calendar2.getTime().getHours() + (long) calendar2.getTime().getMinutes();
        this.flag = flag;
        this.present = 0;
        this.absent = 0;
        this.late = 0;
    }

    public int getPresent() {
        return present;
    }

    public void setPresent(int present) {
        this.present = present;
    }

    public int getAbsent() {
        return absent;
    }

    public void setAbsent(int absent) {
        this.absent = absent;
    }

    public int getLate() {
        return late;
    }

    public void setLate(int late) {
        this.late = late;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public long getId() {
        return id;
    }

    public Long getDayTime() {
        return dayTime;
    }

    public void setDayTime(Long dayTime) {
        this.dayTime = dayTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
}
