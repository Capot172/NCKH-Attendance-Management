package com.atechclass.attendance.model;

import android.graphics.Bitmap;

public class FaceStudent {
    Bitmap imgFace;
    String iDFace;
    String nameFace;
    float[][] embs;

    public FaceStudent(Bitmap imgFace, String iDFace, String nameFace, float[][] embs) {
        this.imgFace = imgFace;
        this.iDFace = iDFace;
        this.nameFace = nameFace;
        this.embs = embs;
    }

    public float[][] getEmbs() {
        return embs;
    }

    public void setEmbs(float[][] embs) {
        this.embs = embs;
    }

    public Bitmap getImgFace() {
        return imgFace;
    }

    public void setImgFace(Bitmap imgFace) {
        this.imgFace = imgFace;
    }

    public String getiDFace() {
        return iDFace;
    }

    public void setiDFace(String iDFace) {
        this.iDFace = iDFace;
    }

    public String getNameFace() {
        return nameFace;
    }

    public void setNameFace(String nameFace) {
        this.nameFace = nameFace;
    }
}
