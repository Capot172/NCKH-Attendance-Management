package com.atechclass.attendance.model;

import android.graphics.Bitmap;

import java.util.Map;

public class DataFace {
    Map<String, Float> emb;
    Bitmap avatar;
    String path;

    public DataFace(Map<String, Float> emb, Bitmap avatar, String path) {
        this.emb = emb;
        this.avatar = avatar;
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, Float> getEmb() {
        return emb;
    }

    public void setEmb(Map<String, Float> emb) {
        this.emb = emb;
    }

    public Bitmap getAvatar() {
        return avatar;
    }

    public void setAvatar(Bitmap avatar) {
        this.avatar = avatar;
    }
}
