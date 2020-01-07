package com.example.landmarkclassification.GUI;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;

public class TrasparentTitleView extends View {
    private static final float TEXT_SIZE_DIP = 24;
    private String mShowText;
    private final float mTextSizePx;
    private final Paint mFgPaint;
    private final Paint mBgPaint;


    public TrasparentTitleView(final Context context, final AttributeSet set) {
        super(context, set);
        mTextSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        mFgPaint = new Paint();
        mFgPaint.setTextSize(mTextSizePx);

        mBgPaint = new Paint();
        mBgPaint.setColor(Color.GRAY);
    }

    @NonNull
    public void setText(@NonNull String text) {
        this.mShowText = text;
        postInvalidate();
    }

    @Override
    public void onDraw(final Canvas canvas) {
        final int x = 10;
        int width = getContext().getResources().getDisplayMetrics().widthPixels;
        int y = (int) (mFgPaint.getTextSize() * 1.5f);
        Paint mClPaint = new Paint();
        mClPaint.setTextSize(mTextSizePx);
        mClPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        canvas.drawPaint(mBgPaint);

        if (mShowText != null) {
            canvas.drawText(mShowText, width/2, y, mClPaint);
            if(mShowText.equals("open")) {
                mBgPaint.setColor(Color.GREEN);
            }
            else if(mShowText.equals("closed")){
                mBgPaint.setColor(Color.RED);
            }
            else if(mShowText.equals("round")){
                mBgPaint.setColor(Color.DKGRAY);
            }
            else if(mShowText.equals("no Face found")){
                mBgPaint.setColor(Color.GRAY);
            }
            else if(mShowText.equals("spitz")) {
                mBgPaint.setColor(Color.GREEN);
            }
            else if(mShowText.equals("not spitz")){
                mBgPaint.setColor(Color.RED);
            }
            else if(mShowText.equals("no Face found")){
                mBgPaint.setColor(Color.GRAY);
            }
        }
    }
}
