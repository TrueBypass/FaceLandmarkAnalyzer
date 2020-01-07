package com.example.landmarkclassification.landmark;

import android.graphics.Bitmap;
import android.graphics.Rect;

public interface CutoutFacePart {

    Bitmap getFacePart(Bitmap frame);
    String getEarresult();
    Rect getBoundingBoxFacepart();
    Rect getBoundingBoxFace();

}
