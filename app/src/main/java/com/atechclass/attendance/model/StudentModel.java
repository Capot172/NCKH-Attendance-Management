package com.atechclass.attendance.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentModel implements Serializable {
    String name;
    String lastName;
    String id;
    String phoneNumber;
    String email;
    Map<String, Integer> lessonMap;
    List<Map<String, Float>> embedding;
    Map<String, Map<String, Float>> embedding2;

    public StudentModel(String name, String id, String phoneNumber, String email, String lastName) {
        this.name = name;
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.lastName = lastName;
        lessonMap = new HashMap<>();
        embedding = new ArrayList<>();
        embedding2 = new HashMap<>();
    }

    public Map<String, Map<String, Float>> getEmbedding2() {
        return embedding2;
    }

    public void setEmbedding2(Map<String, Map<String, Float>> embedding2) {
        this.embedding2 = embedding2;
    }

    public List<Map<String, Float>> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Map<String, Float>> embedding) {
        this.embedding = embedding;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void addLesson(String lesson, Integer state){
        lessonMap.put(lesson,state);
    }

    public void deleteLesson(String lesson){
        lessonMap.remove(lesson);
    }

    public Map<String, Integer> getLessonMap() {
        return lessonMap;
    }

    public void setLessonMap(Map<String, Integer> lessonMap) {
        this.lessonMap = lessonMap;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public StudentModel(){}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public static class Builder {
        private String name = "";
        private String id = "";
        private String phoneNumber = "";
        private String email = "";
        private String lastName = "";

        public Builder name(String name){
            this.name = name;
            return this;
        }
        public Builder id(String id){
            this.id = id;
            return this;
        }
        public Builder phoneNumber(String phoneNumber){
            this.phoneNumber = phoneNumber;
            return this;
        }
        public Builder email(String email){
            this.email = email;
            return this;
        }
        public Builder lastName(String lastName){
            this.lastName = lastName;
            return this;
        }
        public StudentModel build(){
            return  new StudentModel(name, id, phoneNumber, email, lastName);
        }
    }
}
