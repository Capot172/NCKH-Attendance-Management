package com.atechclass.attendance.model;

public class DayTime {
    private String dayTime;
    private String time;

    public DayTime(String dayTime, String time) {
        this.dayTime = dayTime;
        this.time = time;
    }

    public String getDayTime() {
        return dayTime;
    }

    public void setDayTime(String dayTime) {
        this.dayTime = dayTime;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
