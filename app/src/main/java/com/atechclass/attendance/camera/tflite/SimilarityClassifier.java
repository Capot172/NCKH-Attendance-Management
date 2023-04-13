package com.atechclass.attendance.camera.tflite;

import android.graphics.Bitmap;
import java.util.List;

/** Generic interface for interacting with different recognition engines. */
public interface SimilarityClassifier {

    void register(String name, Recognition recognition);

    List<Recognition> recognizeImage(Bitmap bitmap, boolean getExtra);
}
