package com.atechclass.attendance.model;

public class UserUpdate {
    String dayTime;
    String subject;
    String startTime;
    String endTime;
    String address;
    String room;
    long id;
    long idSubject;
    int flag;

    public UserUpdate(String dayTime, String subject, String startTime, String endTime, String address, String room, long id, long idSubject, int flag){
        this.dayTime = dayTime;
        this.subject = subject;
        this.startTime = startTime;
        this.endTime = endTime;
        this.address = address;
        this.room = room;
        this.id = id;
        this.idSubject = idSubject;
        this.flag = flag;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public long getIdSubject() {
        return idSubject;
    }

    public void setIdSubject(long idSubject) {
        this.idSubject = idSubject;
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

    public String getDayTime() {
        return dayTime;
    }

    public void setDayTime(String dayTime) {
        this.dayTime = dayTime;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
