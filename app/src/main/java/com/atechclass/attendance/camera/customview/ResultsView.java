package com.atechclass.attendance.camera.customview;

import com.atechclass.attendance.camera.tflite.Recognition;

import java.util.List;

public interface ResultsView {
    public void setResults(final List<Recognition> results);
}

