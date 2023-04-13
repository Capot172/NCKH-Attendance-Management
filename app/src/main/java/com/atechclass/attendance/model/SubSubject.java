package com.atechclass.attendance.model;

public class SubSubject {
    String session;
    String startTime;
    String startEnd;
    String dayTime;
    String ratio;
    int participation;
    int present;
    int absent;

    public int getNoAttendance() {
        return noAttendance;
    }

    public void setNoAttendance(int noAttendance) {
        this.noAttendance = noAttendance;
    }

    int noAttendance;
    public  SubSubject(){}
    public SubSubject(String session, String startTime, String startEnd, String dayTime, String ratio, int participation, int present, int absent, int noAttendance) {
        this.session = session;
        this.startTime = startTime;
        this.startEnd = startEnd;
        this.dayTime = dayTime;
        this.ratio = ratio;
        this.participation = participation;
        this.present = present;
        this.absent = absent;
        this.noAttendance = noAttendance;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getStartEnd() {
        return startEnd;
    }

    public void setStartEnd(String startEnd) {
        this.startEnd = startEnd;
    }

    public String getDayTime() {
        return dayTime;
    }

    public void setDayTime(String dayTime) {
        this.dayTime = dayTime;
    }

    public String getRatio() {
        return ratio;
    }

    public void setRatio(String ratio) {
        this.ratio = ratio;
    }

    public int getParticipation() {
        return participation;
    }

    public void setParticipation(int participation) {
        this.participation = participation;
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
}
