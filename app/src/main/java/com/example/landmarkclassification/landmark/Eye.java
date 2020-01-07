package com.example.landmarkclassification.landmark;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;

import androidx.annotation.Nullable;

import com.example.landmarkclassification.LandmarkConstants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;

import java.util.ArrayList;
import java.util.List;

import static com.example.landmarkclassification.Util.MathUtils.distance;

public class Eye implements CutoutFacePart {
    private static final String TAG = "landmarkTest";
    private static final Double EYE_AR_THRESH = 0.23;
    private FaceDet mFaceDet = FaceDetSingleton.getInstance();
    private int mScope = 0;
    private Rect mBoundsFacepart = new Rect();
    private Rect mBoundsFace = new Rect();
    private Double mEar = 0.0;


    @Override
    @Nullable
    public Bitmap getFacePart(Bitmap frame) {
        ArrayList<Point> landmarks;
        List<VisionDetRet> results = mFaceDet.detect(frame);
        if(results.size() != 0) {
            landmarks = results.get(0).getFaceLandmarks();
            int yCorner = landmarks.get(37).y - mScope;
            int xCorner = landmarks.get(36).x - mScope;
            int heighhRec = landmarks.get(41).y - yCorner + mScope;
            int widthRec  = landmarks.get(39).x - xCorner + mScope ;
            setBoundingBoxFacepart(landmarks);
            setBoundingBoxFace(results.get(0));
            computeEAR(landmarks);
            if(xCorner > 0 && yCorner > 0 && widthRec > 0 && heighhRec > 0 ) {
                Bitmap croppedBmp = Bitmap.createBitmap(frame, xCorner, yCorner, widthRec, heighhRec);
                return Bitmap.createScaledBitmap(croppedBmp, LandmarkConstants.IMAGE_SIZE, LandmarkConstants.IMAGE_SIZE, false);
            }
        }
        return null;
    }

    @Override
    public String getEarresult(){
        return mEar < EYE_AR_THRESH ? "closed" : "open";
    }

    @Override
    public Rect getBoundingBoxFacepart() {
        return mBoundsFacepart;
    }

    @Override
    public Rect getBoundingBoxFace() {
        return mBoundsFace;
    }

    private void computeEAR(ArrayList<Point> landmarks) {
        Point[] eye = {landmarks.get(36), landmarks.get(37), landmarks.get(38), landmarks.get(39), landmarks.get(40), landmarks.get(41)};
        double a = distance(eye[1], eye[5]);
        double b = distance(eye[2], eye[4]);
        double c = distance(eye[0], eye[3]);
        mEar = (a + b) / (2.0 * c);

    }

    private void setBoundingBoxFacepart(ArrayList<Point> landmarks) {
        mBoundsFacepart.left = landmarks.get(36).x - mScope;
        mBoundsFacepart.top = landmarks.get(37).y - mScope;
        mBoundsFacepart.right = landmarks.get(39).x + mScope;
        mBoundsFacepart.bottom = landmarks.get(40).y + mScope ;

    }

    private void setBoundingBoxFace(VisionDetRet visionDetRet) {
        mBoundsFace.left = visionDetRet.getLeft();
        mBoundsFace.top = visionDetRet.getTop();
        mBoundsFace.right = visionDetRet.getRight();
        mBoundsFace.bottom = visionDetRet.getBottom();

    }

    
}
