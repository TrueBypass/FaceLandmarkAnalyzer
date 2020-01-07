package com.example.landmarkclassification.GUI;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.UiThread;

import com.example.landmarkclassification.OnGetImageListener;
import com.example.landmarkclassification.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;


public class FloatingCameraWindow {
    private static final String TAG = "FloatingCameraWindow";
    private Context mContext;
    private WindowManager.LayoutParams mWindowParam;
    private WindowManager mWindowManager;
    private FloatCamView mRootView;
    private Handler mUIHandler;
    private OnGetImageListener mOnGetImageListener;

    private int mWindowWidth;
    private int mWindowHeight;

    private int mScreenMaxWidth;
    private int mScreenMaxHeight;

    private float mScaleWidthRatio = 1.0f;
    private float mScaleHeightRatio = 1.0f;

    private static final boolean DEBUG = true;

    public FloatingCameraWindow(Context context, OnGetImageListener onGetImageListener) {
        mContext = context;
        mUIHandler = new Handler(Looper.getMainLooper());
        mOnGetImageListener = onGetImageListener;

        // Get screen max size
        Point size = new Point();
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            display.getSize(size);
            mScreenMaxWidth = size.x;
            mScreenMaxHeight = size.y;
        } else {
            mScreenMaxWidth = display.getWidth();
            mScreenMaxHeight = display.getHeight();
        }
        // Default window size
        mWindowHeight = mScreenMaxHeight;
        mWindowWidth = mScreenMaxWidth;
    }

    private void init() {
        mUIHandler.postAtFrontOfQueue(new Runnable() {
            @Override
            public void run() {
                if (mWindowManager == null || mRootView == null) {
                    mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
                    mRootView = new FloatCamView(FloatingCameraWindow.this);
                    mWindowManager.addView(mRootView, initWindowParameter());
                }
            }
        });
    }

    public void release() {
        mUIHandler.postAtFrontOfQueue(new Runnable() {
            @Override
            public void run() {
                if (mWindowManager != null) {
                    mWindowManager.removeViewImmediate(mRootView);
                    mRootView = null;
                }
                mUIHandler.removeCallbacksAndMessages(null);
            }
        });
    }

    private WindowManager.LayoutParams initWindowParameter() {
        mWindowParam = new WindowManager.LayoutParams();

        mWindowParam.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        mWindowParam.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mWindowParam.flags = mWindowParam.flags | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        mWindowParam.flags = mWindowParam.flags | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

        mWindowParam.x = 0;
        mWindowParam.y = 70;
        return mWindowParam;
    }

    public void setRGBBitmap(final Bitmap rgb) {
        checkInit();
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                mRootView.setRGBImageView(rgb);
            }
        });
    }

    public void setLanmarkTimecost(final String timecost) {
        checkInit();
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                checkInit();
                mRootView.setLandmarkTimecost(timecost);
            }
        });
    }

    public void setClassifierTimecost(final String timecost) {
        checkInit();
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                checkInit();
                mRootView.setClassifierTimecost(timecost);
            }
        });
    }

    private void checkInit() {
        if (mRootView == null) {
            init();
        }
    }

    @UiThread
    private final class FloatCamView extends FrameLayout {
        private WeakReference<FloatingCameraWindow> mWeakRef;
        private static final int MOVE_THRESHOLD = 10;
        private int mLastX;
        private int mLastY;
        private int mFirstX;
        private int mFirstY;
        private LayoutInflater mLayoutInflater;
        private ImageView mColorView;
        private TextView mLandmarkTimecostText;
        private Spinner mLandmarkItemSpinner;
        private Spinner mClassifierSpinner;
        private ArrayList<String> mLandmarkItems = new ArrayList<>(Arrays.asList("Eye", "Mouth", "Mouthclosing", "Change Facepart"));
        private ArrayList<String> mClassifierMethod = new ArrayList<>(Arrays.asList("EAR", "CNN", "Change Classifier"));
        private TextView mClassifierTimecostText;
        private boolean mIsMoving = false;


        public FloatCamView(FloatingCameraWindow window) {
            super(window.mContext);
            mWeakRef = new WeakReference<FloatingCameraWindow>(window);
            mLayoutInflater = (LayoutInflater) window.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            FrameLayout body = (FrameLayout) this;
            body.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
            });

            View floatView = mLayoutInflater.inflate(R.layout.cam_window_view, body, true);
            mColorView = findViewById(R.id.imageView_c);
            mLandmarkTimecostText = findViewById(R.id.landmark_timecost);
            mLandmarkItemSpinner = findViewById(R.id.spinnerLandmarkItem);
            mClassifierSpinner = findViewById(R.id.spinnerClassifier);
            mClassifierTimecostText = findViewById(R.id.classifier_timecost);

            int colorMaxWidth = (int) (mWindowWidth * window.mScaleWidthRatio);
            int colorMaxHeight = (int) (mWindowHeight * window.mScaleHeightRatio);

            mColorView.getLayoutParams().width = colorMaxWidth;
            mColorView.getLayoutParams().height = colorMaxHeight;


            HintAdapter hintAdapterLandmarkItem = new HintAdapter(getContext(), R.layout.my_spinner_layout, mLandmarkItems);
            mLandmarkItemSpinner.setAdapter(hintAdapterLandmarkItem);
            mLandmarkItemSpinner.setSelection(hintAdapterLandmarkItem.getCount());
            AdapterView.OnItemSelectedListener onItemSelectedListenerLandmarkItem = new AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> spinner, View container, int position, long id) {
                    Log.i("Spinner", "position: " + position);
                    mOnGetImageListener.landmarkItemChanged(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                    // TODO Auto-generated method stub
                }
            };

            mLandmarkItemSpinner.setOnItemSelectedListener(onItemSelectedListenerLandmarkItem);


            HintAdapter hintAdapterClassifierMethod = new HintAdapter(getContext(), R.layout.my_spinner_layout, mClassifierMethod);
            mClassifierSpinner.setAdapter(hintAdapterClassifierMethod);

            mClassifierSpinner.setSelection(hintAdapterClassifierMethod.getCount());
            AdapterView.OnItemSelectedListener onItemSelectedListenerClassifier = new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View container, int position, long id) {
                    mOnGetImageListener.ClassifierChanged(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            };

            mClassifierSpinner.setOnItemSelectedListener(onItemSelectedListenerClassifier);


        }

        public void setRGBImageView(Bitmap rgb) {
            if (rgb != null && !rgb.isRecycled()) {
                mColorView.setImageBitmap(rgb);
            }
        }

        public void setLandmarkTimecost(String fps) {
            if (mLandmarkTimecostText != null) {
                if (mLandmarkTimecostText.getVisibility() == View.GONE) {
                    mLandmarkTimecostText.setVisibility(View.VISIBLE);
                }
                mLandmarkTimecostText.setText(fps);
            }
        }

        public void setClassifierTimecost(String info) {
            if (mLandmarkTimecostText != null) {
                if (mClassifierTimecostText.getVisibility() == View.GONE) {
                    mClassifierTimecostText.setVisibility(View.VISIBLE);
                }
                mClassifierTimecostText.setText(info);
            }
        }
    }

}
