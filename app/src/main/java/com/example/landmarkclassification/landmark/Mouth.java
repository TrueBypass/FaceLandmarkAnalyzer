package com.example.landmarkclassification.landmark;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.landmarkclassification.LandmarkConstants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;

import java.util.ArrayList;
import java.util.List;

import static com.example.landmarkclassification.Util.MathUtils.distance;

public class Mouth implements CutoutFacePart {
    private static final String TAG = "landmarkTest";
    private static final Double MOUTH_AR_THRESH = 0.13;
    private FaceDet mFaceDet = FaceDetSingleton.getInstance();
    private int mScope = 10;
    protected Double mEar = 0.0;
    private Rect mBoundsFacepart = new Rect();
    private Rect mBoundsFace = new Rect();

    public Mouth(){
        Log.i("Test", "Mouth created");
    }

    @Override
    @Nullable
    public Bitmap getFacePart(Bitmap frame) {
        ArrayList<Point> landmarks;
        List<VisionDetRet> results = mFaceDet.detect(frame);
        if(results.size() != 0) {
            landmarks = results.get(0).getFaceLandmarks();
            int yCorner = landmarks.get(50).y;
            int xCorner = landmarks.get(48).x;
            int heighhRec = landmarks.get(57).y + (landmarks.get(62).y - landmarks.get(51).y) - yCorner;
            int widthRec = landmarks.get(54).x - xCorner;
            computeEAR(landmarks);
            setBoundingBoxFacepart(landmarks);
            setBoundingBoxFace(results.get(0));
            if(xCorner > 0 && yCorner > 0 && widthRec > 0 && heighhRec > 0 ) {
                Bitmap croppedBmp = Bitmap.createBitmap(frame, xCorner, yCorner, widthRec, heighhRec);
                return Bitmap.createScaledBitmap(croppedBmp, LandmarkConstants.IMAGE_SIZE, LandmarkConstants.IMAGE_SIZE, false);
            }
        }
        return null;
    }

    @Override
    public String getEarresult() {
        return mEar >= MOUTH_AR_THRESH ? "open" : "closed";
    }

    @Override
    public Rect getBoundingBoxFacepart() {
        return mBoundsFacepart;
    }

    @Override
    public Rect getBoundingBoxFace() {
        return mBoundsFace;
    }

    protected void computeEAR(ArrayList<Point> landmarks) {
        Point[] mouth = {landmarks.get(60), landmarks.get(61), landmarks.get(62), landmarks.get(63), landmarks.get(64), landmarks.get(65), landmarks.get(66), landmarks.get(67)};
        double a = distance(mouth[1], mouth[7]);
        double b = distance(mouth[2], mouth[6]);
        double c = distance(mouth[0], mouth[4]);
        mEar = (a + b) / (2.0 * c);

    }

    protected void setBoundingBoxFacepart(ArrayList<Point> landmarks) {
        mBoundsFacepart.left = landmarks.get(48).x - mScope;
        mBoundsFacepart.top = landmarks.get(50).y - mScope;
        mBoundsFacepart.right = landmarks.get(54).x + mScope;
        mBoundsFacepart.bottom = landmarks.get(57).y + mScope ;

    }

    protected void setBoundingBoxFace(VisionDetRet visionDetRet) {
        mBoundsFace.left = visionDetRet.getLeft();
        mBoundsFace.top = visionDetRet.getTop();
        mBoundsFace.right = visionDetRet.getRight();
        mBoundsFace.bottom = visionDetRet.getBottom();

    }

}
