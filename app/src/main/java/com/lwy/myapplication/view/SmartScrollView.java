package com.lwy.myapplication.view;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import com.lwy.myapplication.base.IScrollListener;
import com.lwy.myapplication.base.IScrollSubscription;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static android.view.MotionEvent.ACTION_DOWN;

/**
 * description ：
 * author : lwy
 * email :  9011532@qq.com
 * date : 2019/12/18
 */
public class SmartScrollView extends ScrollView implements IScrollSubscription {
    public static final int UP = 0;
    public static final int DOWN = 1;
    private static final long sCheckEndTimePeriod = 10;

    private OnScrollListener onScrollListener;

    private Handler mHandler;
    private int scrollY;
    private GestureDetector gestureDector;
    private boolean isFling;

    @Override
    public int getVisiableHeight() {
        return getHeight();
    }

    private static class InnerHandler extends Handler {
        WeakReference<SmartScrollView> ref;

        public InnerHandler(SmartScrollView customScrollView) {
            ref = new WeakReference<>(customScrollView);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                if (ref.get() != null && ref.get().isfinishScroll()) {
                    if (ref.get().onScrollListener != null) {
                        ref.get().onScrollListener.onScrollEnd();
                        ref.get().isFling = false;
                    }
                } else {
                    sendEmptyMessageDelayed(0, sCheckEndTimePeriod);
                }
            }
        }
    }

    public SmartScrollView(Context context) {
        this(context, null);
    }

    public SmartScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SmartScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mHandler = new InnerHandler(this);
        gestureDector = new GestureDetector(context, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                isFling = true;
                return false;
            }
        });
    }

    /**
     * 设置滚动接口
     *
     * @param onScrollListener
     */
    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.onScrollListener = onScrollListener;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        scrollY = getScrollY();
        int direction = t - oldt > 0 ? UP : DOWN;
        if (onScrollListener != null) {
            onScrollListener.onScroll(scrollY, direction);
        }
        notifyChildListenerScrollChanged();
    }

    private void notifyChildListenerScrollChanged() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            List<IScrollListener> list = findIScrollListenerByFor(child);
            for (IScrollListener listener : list) {
                listener.onScrollChanged(getScrollX(), getScrollY());
            }
        }
    }

    private void notifyChildListenerSizeChanged(int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            List<IScrollListener> list = findIScrollListenerByFor(child);
            for (IScrollListener listener : list) {
                listener.onVisibleSizeChanged(r - l - getPaddingLeft() - getPaddingRight(),
                        b - t - getPaddingBottom() - getPaddingTop());
            }
        }
    }

    private List<IScrollListener> findIScrollListenerByFor(View view) {
        List<IScrollListener> retList = new ArrayList();
        if (view instanceof IScrollListener) {
            retList.add((IScrollListener) view);
        } else if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                if (child instanceof IScrollListener) {
                    retList.add((IScrollListener) child);
                } else if (child instanceof ViewGroup) {
                    List tempList = findIScrollListenerByFor((ViewGroup) child);
                    retList.addAll(tempList);
                }
            }
        }
        return retList;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        notifyChildListenerSizeChanged(l, t, r, b);
        super.onLayout(changed, l, t, r, b);
        if (this.onScrollListener != null) {
            View child = getChildAt(0);
            if (child != null) {
                int childHeight = child.getHeight();
                int maxHeight = childHeight + getPaddingTop() + getPaddingBottom();
                this.onScrollListener.onMaxHeightChanged(maxHeight);
            }
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        gestureDector.onTouchEvent(ev);
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_UP:
                if (isFling) {
                    mHandler.sendEmptyMessageDelayed(0, sCheckEndTimePeriod);
                } else {
                    if (this.onScrollListener != null) {
                        this.onScrollListener.onScrollEnd();
                    }
                }
                break;
            case ACTION_DOWN:
                if (onScrollListener != null) {
                    onScrollListener.onScrollStart();
                }
                mHandler.removeMessages(0);
                break;

        }
        return super.onTouchEvent(ev);

    }

    public boolean isfinishScroll() {
        boolean isfinish = false;
        Class scrollview = ScrollView.class;
        try {
            Field scrollField = scrollview.getDeclaredField("mScroller");
            scrollField.setAccessible(true);
            Object scroller = scrollField.get(this);
            Class overscroller = scrollField.getType();
            Method finishField = overscroller.getMethod("isFinished");
            finishField.setAccessible(true);
            isfinish = (boolean) finishField.invoke(scroller);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {

        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return isfinish;

    }

    public interface OnScrollListener {
        /**
         * 回调方法， 返回CustomScrollView滑动的Y方向距离
         *
         * @param scrollY
         * @param direction
         */
        public void onScroll(int scrollY, int direction);

        public void onScrollStart();

        public void onScrollEnd();

        /**
         * 当SmartScrollView的内容最大高度发生变化时回调
         *
         * @param maxHeight
         */
        public void onMaxHeightChanged(int maxHeight);

    }


}