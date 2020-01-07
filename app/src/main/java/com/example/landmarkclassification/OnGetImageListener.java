package com.example.landmarkclassification;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.example.landmarkclassification.Util.ImageUtilsNotNative;
import com.example.landmarkclassification.landmark.Mouthclosing;
import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.example.landmarkclassification.GUI.FloatingCameraWindow;
import com.example.landmarkclassification.GUI.TrasparentTitleView;
import com.example.landmarkclassification.TfLite.Classifier;
import com.example.landmarkclassification.Util.FileUtils;
import com.example.landmarkclassification.landmark.CutoutFacePart;
import com.example.landmarkclassification.landmark.Eye;
import com.example.landmarkclassification.landmark.Mouth;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that takes in preview frames and converts the image to Bitmaps to process with dlib lib.
 */
public class OnGetImageListener implements OnImageAvailableListener {
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final String TAG = "OnGetImageListener";

    private int mScreenRotation = 90;
    private int mPreviewWdith = 0;
    private int mPreviewHeight = 0;
    private Bitmap mRGBframeBitmap = null;
    private Bitmap mCroppedBitmap = null;
    private Bitmap mCroppedFacepart = null;
    private List<Classifier.Recognition> mClassifierResults;
    private String mFinalResult;
    private long mStartTimeLandmark = 0;
    private long mEndTimeLandmark = 0;
    private long mStartTimeClassifier = 0;
    private long mEndTimeClassifier = 0;

    private boolean mIsProcessingFrame = false;
    private boolean mIsEar = true;
    private Handler mInferenceHandler;

    private Activity mActivity;
    private Context mContext;
    private FaceDet mFaceDet;
    private Eye mEye;
    private Mouth mMouth;
    private Mouthclosing mMouthclosing;
    private CutoutFacePart mFacepart;
    private Classifier mClassifier;
    private Classifier mMouthClassifier;
    private Classifier mEyeClassifier;
    private TrasparentTitleView mTransparentTitleView;
    private FloatingCameraWindow mWindow;
    private Paint mFaceLandmardkPaint;

    private int yRowStride;
    private byte[][] mYUVBytes = new byte[3][];
    private int[] mRGBbytes = null;

    public void initialize(
            final Activity activity,
            final Context context,
            final AssetManager assetManager,
            final TrasparentTitleView scoreView,
            final Handler handler) {
        this.mActivity = activity;
        this.mContext = context;
        this.mTransparentTitleView = scoreView;
        this.mInferenceHandler = handler;
        mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());
        mEye = new Eye();
        mMouth = new Mouth();
        mMouthclosing = new Mouthclosing();
        mFacepart = mMouth;
        mWindow = new FloatingCameraWindow(mContext, this);

        try {
            mMouthClassifier = recreateClassifiers(mMouthClassifier, Classifier.ClassifierType.MOUTH, 2);
            mEyeClassifier = recreateClassifiers(mEyeClassifier, Classifier.ClassifierType.EYE, 2);
        } catch (Exception e) {
            Log.i(TAG, e.getMessage() + "could not loud TensorFlow Lite");
        }

        mFaceLandmardkPaint = new Paint();
        mFaceLandmardkPaint.setColor(Color.GREEN);
        mFaceLandmardkPaint.setStrokeWidth(2);
        mFaceLandmardkPaint.setStyle(Paint.Style.STROKE);
    }

    public void deInitialize() {
        synchronized (OnGetImageListener.this) {
            if (mFaceDet != null) {
                mFaceDet.release();
            }

            if (mWindow != null) {
                mWindow.release();
            }
        }
    }

    private Classifier recreateClassifiers(Classifier classifier, Classifier.ClassifierType classifierType, int numThreads) {
        if (classifier != null) {
            Log.i(TAG, "Closing classifier.");
            classifier.close();
            classifier = null;
        }
        try {
            Log.i(TAG, "Creating classifier");
            long startTimeClassifier = SystemClock.uptimeMillis();
            classifier = Classifier.create(mActivity, numThreads, classifierType);
            long endTimeClassifier = SystemClock.uptimeMillis() - startTimeClassifier;
            Log.i(TAG, "time create classifier: " + endTimeClassifier);
            return classifier;
        } catch (IOException e) {
            Log.i(TAG, "Failed to create classifier.");
            Log.i(TAG, e.getMessage());
        }
        return null;
    }

    private String reshapeResult(String results) {
        Pattern p = Pattern.compile(" (.*?) ");
        Matcher m = p.matcher(results);
        String result = "";

        while (m.find()) {
            return m.group(1);
        }
        return result;
    }

    private void drawResizedBitmap(final Bitmap src, final Bitmap dst) {

        Display getOrient = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int orientation = Configuration.ORIENTATION_UNDEFINED;
        Point point = new Point();
        getOrient.getSize(point);
        int screen_width = point.x;
        int screen_height = point.y;
        Log.d(TAG, String.format("screen size (%d,%d)", screen_width, screen_height));
        if (screen_width < screen_height) {
            //orientation = Configuration.ORIENTATION_PORTRAIT;
            //mScreenRotation = 90;
        } else {
            orientation = Configuration.ORIENTATION_LANDSCAPE;
            mScreenRotation = 0;
        }

        // Assert.assertEquals(dst.getWidth(), dst.getHeight());
        final float minDim = Math.min(src.getWidth(), src.getHeight());

        final Matrix matrix = new Matrix();

        // We only want the center square out of the original rectangle.
        final float translateX = -Math.max(0, (src.getWidth() - minDim) / 2);
        final float translateY = -Math.max(0, (src.getHeight() - minDim) / 2);
        matrix.preTranslate(translateX, translateY);
        // matrix.preTranslate(src.getWidth(), src.getHeight());


        final float scaleFactor = dst.getHeight() / minDim;
        // matrix.postScale(scaleFactor, scaleFactor);


        // Rotate around the center if necessary.
        if (mScreenRotation != 0) {
            matrix.postTranslate(-dst.getWidth() / 2.0f, -dst.getHeight() / 2.0f);
            matrix.postRotate(mScreenRotation);
            matrix.postTranslate(dst.getWidth() / 2.0f, dst.getHeight() / 2.0f);
        }

        final Canvas canvas = new Canvas(dst);
        canvas.drawBitmap(src, matrix, null);
    }

    protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    @Override
    public void onImageAvailable(final ImageReader reader) {
        // We need wait until we have some size from onPreviewSizeChosen

        if (mRGBbytes == null) {
            mRGBbytes = new int[mPreviewWdith * mPreviewHeight];
        }
        try {
            final Image image = reader.acquireLatestImage();

            if (image == null) {
                Log.i(TAG, "Image is null");
                return;
            }

            if (mIsProcessingFrame) {
                image.close();
                return;
            }
            mIsProcessingFrame = true;
            Trace.beginSection("imageAvailable");
            final Plane[] planes = image.getPlanes();
            if (mPreviewWdith != image.getWidth() || mPreviewHeight != image.getHeight()) {
                mPreviewWdith = image.getWidth();
                mPreviewHeight = image.getHeight();
                mRGBbytes = new int[mPreviewWdith * mPreviewHeight];
                mRGBframeBitmap = Bitmap.createBitmap(mPreviewWdith, mPreviewHeight, Config.ARGB_8888);
                mCroppedBitmap = Bitmap.createBitmap(LandmarkConstants.IMAGE_SIZE, LandmarkConstants.IMAGE_SIZE, Config.ARGB_8888);
            }
            fillBytes(planes, mYUVBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();


            ImageUtilsNotNative.convertYUV420ToARGB8888(
                    mYUVBytes[0],
                    mYUVBytes[1],
                    mYUVBytes[2],
                    mPreviewWdith,
                    mPreviewHeight,
                    yRowStride,
                    uvRowStride,
                    uvPixelStride,
                    mRGBbytes);

            image.close();


        } catch (final Exception e) {
            Log.e(TAG, "Exception!", e);
            Trace.endSection();
            return;
        }
        Trace.endSection();

        mRGBframeBitmap.setPixels(mRGBbytes, 0, mPreviewWdith, 0, 0, mPreviewWdith, mPreviewHeight);

        //drawResizedBitmap(mRGBframeBitmap, mCroppedBitmap);
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtilsNotNative.saveBitmap(mRGBframeBitmap);

        }

        mInferenceHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        if (!new File(Constants.getFaceShapeModelPath()).exists()) {
                            FileUtils.copyFileFromRawToOthers(mContext, R.raw.shape_predictor_68_face_landmarks, Constants.getFaceShapeModelPath());
                            return;
                        }

                        synchronized (OnGetImageListener.this) {
                            mStartTimeLandmark = System.currentTimeMillis();
                            mCroppedFacepart = mFacepart.getFacePart(mRGBframeBitmap);
                            mEndTimeLandmark = System.currentTimeMillis();

                            if (mCroppedFacepart != null) {
                                mStartTimeClassifier = System.currentTimeMillis();
                                if (mIsEar) {
                                    mFinalResult = mFacepart.getEarresult();
                                    mTransparentTitleView.setText(mFinalResult);
                                    Log.i(TAG, mFinalResult);
                                } else {
                                    mClassifierResults = mClassifier.recognizeImage(mCroppedFacepart);
                                    mFinalResult = reshapeResult(mClassifierResults.toString());
                                    mTransparentTitleView.setText(mFinalResult);
                                    Log.i(TAG, mFinalResult);
                                }
                                mEndTimeClassifier = System.currentTimeMillis();
                            } else {
                                mTransparentTitleView.setText("no Face found");
                            }
                        }

                        mWindow.setLanmarkTimecost("Time cost Face- and Landmarkdetection: " + String.valueOf((mEndTimeLandmark - mStartTimeLandmark) / 1000f) + " sec");
                        mWindow.setClassifierTimecost("Timecost" + (mIsEar ? "EAR" : "CNN") + " Classifier: " + String.valueOf((mEndTimeClassifier - mStartTimeClassifier) / 1000f) + " sec");

                        // Draw on bitmap
                        if (mCroppedFacepart != null) {
                            Canvas canvas = new Canvas(mRGBframeBitmap);
                            canvas.drawRect(mFacepart.getBoundingBoxFace(), mFaceLandmardkPaint);
                            canvas.drawRect(mFacepart.getBoundingBoxFacepart(), mFaceLandmardkPaint);
                        } else {
                            Log.i(TAG, "no Face found");
                        }

                        mWindow.setRGBBitmap(mRGBframeBitmap);
                        mIsProcessingFrame = false;
                    }
                });

        Trace.endSection();
    }

    public void landmarkItemChanged(int position) {
        switch (position) {
            case 0:
                mFacepart = mEye;
                mClassifier = mEyeClassifier;
                break;
            case 1:
                mFacepart = mMouth;
                mClassifier = mMouthClassifier;
                break;
            case 2:
                mFacepart = mMouthclosing;
                mClassifier = mMouthClassifier;
                break;
            default:
                mFacepart = mEye;
                mClassifier = mEyeClassifier;
                break;
        }
    }

    public void ClassifierChanged(int position) {
        switch (position) {
            case 0:
                mIsEar = true;
                break;
            case 1:
                mIsEar = false;
                break;
            default:
                mIsEar = true;
                break;
        }
    }
}
