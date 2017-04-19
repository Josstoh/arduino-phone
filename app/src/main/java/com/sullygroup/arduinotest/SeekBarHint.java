package com.sullygroup.arduinotest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.SeekBar;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jocelyn.caraman on 07/03/2017.
 */

public class SeekBarHint extends AppCompatSeekBar implements SeekBar.OnSeekBarChangeListener{
    Paint mTxtPaint;
    String progress;
    Paint.FontMetrics fm;
    Paint rec;

    boolean isMoving = false;

    private List<OnSeekBarChangeListener> listeners;

    public SeekBarHint(Context context) {
        super(context);
        init();
    }

    public SeekBarHint(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SeekBarHint(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        listeners = new ArrayList<>();
        super.setOnSeekBarChangeListener(this);
        mTxtPaint = new Paint();
        fm = new Paint.FontMetrics();
        mTxtPaint.setColor(Color.BLACK);
        mTxtPaint.setTextSize(25f);
        mTxtPaint.getFontMetrics(fm);

        rec = new Paint();
        rec.setColor(Color.BLACK);
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        addOnSeekBarChangeListener(l);
    }

    public void addOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        for (OnSeekBarChangeListener listener : listeners) {
            listener.onProgressChanged(seekBar, progress, fromUser);
        }

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        for (OnSeekBarChangeListener listener : listeners) {
            listener.onStartTrackingTouch(seekBar);
        }

        isMoving = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        for (OnSeekBarChangeListener listener : listeners) {
            listener.onStopTrackingTouch(seekBar);
        }
        isMoving = false;
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        MainActivity activity = (MainActivity) getContext();
        if(isMoving){

            int thumb_x = (int)(( (double)this.getProgress()/this.getMax() ) * (double)this.getWidth());
            int width = this.getWidth()
                    - this.getPaddingLeft()
                    - this.getPaddingRight();
            int thumbPos = this.getPaddingLeft()
                    + width
                    * this.getProgress()
                    / this.getMax();

            int middle = this.getHorizontalScrollbarHeight();
            progress = String.valueOf(this.getProgress());

            activity.hintSeekBar.setText(progress);
            activity.hintSeekBar.measure(0,0);
            thumbPos -= activity.hintSeekBar.getMeasuredWidth() / 2 + 1;
            activity.hintSeekBar.setX(thumbPos);
            activity.hintSeekBar.setY(middle);
            activity.hintSeekBar.setVisibility(VISIBLE);
        }
        else
            activity.hintSeekBar.setVisibility(GONE);

    }
}
