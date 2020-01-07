package com.example.landmarkclassification.Util;

import android.graphics.Point;

public class MathUtils {

    public static double distance (Point point1, Point point2) {
        return Math.sqrt((point1.x - point2.x)*(point1.x - point2.x) + (point1.y - point2.y)*(point1.y - point2.y));
    }
}

