package com.picpicker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SwipeInterceptorLayout extends FrameLayout {

    public static final int DRAG_START_THRESHOLD = 20;
    public static final int COMMIT_THRESHOLD = 200;
    private static final float DIRECTION_RATIO = 1.2f;

    private float startX;
    private float startY;
    private boolean isVerticalDrag;
    private OnSwipeListener onSwipeListener;

    public interface OnSwipeListener {
        void onSwipeUp();
        void onSwipeDown();
        void onDrag(float deltaY);
        void onDragReset();
    }

    public SwipeInterceptorLayout(@NonNull Context context) {
        super(context);
    }

    public SwipeInterceptorLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SwipeInterceptorLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnSwipeListener(OnSwipeListener listener) {
        this.onSwipeListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = ev.getX();
                startY = ev.getY();
                isVerticalDrag = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!isVerticalDrag) {
                    float dx = ev.getX() - startX;
                    float dy = ev.getY() - startY;
                    if (Math.abs(dy) > DRAG_START_THRESHOLD && Math.abs(dy) > Math.abs(dx) * DIRECTION_RATIO) {
                        isVerticalDrag = true;
                        return true;
                    }
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (isVerticalDrag) {
            float deltaY = ev.getY() - startY;
            switch (ev.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    if (onSwipeListener != null) {
                        onSwipeListener.onDrag(deltaY);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    isVerticalDrag = false;
                    if (onSwipeListener != null) {
                        if (Math.abs(deltaY) > COMMIT_THRESHOLD) {
                            if (deltaY < 0) {
                                onSwipeListener.onSwipeUp();
                            } else {
                                onSwipeListener.onSwipeDown();
                            }
                        } else {
                            onSwipeListener.onDragReset();
                        }
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    isVerticalDrag = false;
                    if (onSwipeListener != null) {
                        onSwipeListener.onDragReset();
                    }
                    return true;
            }
            return true;
        }
        return super.onTouchEvent(ev);
    }
}
