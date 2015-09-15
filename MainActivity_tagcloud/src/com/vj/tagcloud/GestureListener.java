package com.vj.tagcloud;

import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class GestureListener extends SimpleOnGestureListener {
    private View trainView;
 
    public GestureListener(View view) {
        this.trainView = view;
    }
 
    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }
 
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        trainView.scrollBy((int)distanceX,(int)distanceY);
        return true;
    }
 
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
       // trainView.fling((int) velocityX);
        return true;
    }
}