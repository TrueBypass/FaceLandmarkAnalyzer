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

public class Mouthclosing implements CutoutFacePart {
    private static final String TAG = "landmarkTest";
    private static final Double MOUTH_AR_THRESH = 0.13;
    private FaceDet mFaceDet = FaceDetSingleton.getInstance();
    private int mScope = 10;
    protected Double mEar = 0.0;
    private Rect mBoundsFacepart = new Rect();
    private Rect mBoundsFace = new Rect();

    public Mouthclosing(){
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
        if(mEar == 1.0){
            return "spitz";
        }
        if(mEar == 0.0){
            return "not spitz";
        }
        if(mEar == -1.0){
            return "no Face found";
        }
        return "Error";
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
        int heightLeft;
        int heightRight;
        double height;
        double lipheight;

        if (landmarks != null) {
            heightLeft = landmarks.get(60).y;
            heightRight = landmarks.get(64).y;
            height = heightLeft > heightRight ? heightLeft : heightRight;
            lipheight = landmarks.get(61).y - landmarks.get(50).y;
            height = height - 0.5 *lipheight;
            Log.i("VideoResultComputer", "lipheight: "+ lipheight + "  9%: " + height*0.09);


            for(int i = 65; i<=67; i++){
                Log.i("VideoResultComputer", "Hoehe: " + height + " current Landmark " + i + " :  " + landmarks.get(i).y);
                if(landmarks.get(i).y < height){
                    Log.i("VideoResultComputer", "spitz");
                    mEar = 1.0;
                    return;
                }
            }
            mEar = 0.0;
        } else {
            mEar = -1.0;
        }

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
