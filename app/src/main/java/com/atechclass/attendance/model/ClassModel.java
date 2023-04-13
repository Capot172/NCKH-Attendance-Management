package com.atechclass.attendance.model;

import java.io.Serializable;

public class ClassModel implements Serializable {

    String subject;
    int id;

    public ClassModel(String subject) {
        this.subject = subject;
        this.id = (int) (System.currentTimeMillis() & 0xfffffff);
    }
    public ClassModel(){}

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
