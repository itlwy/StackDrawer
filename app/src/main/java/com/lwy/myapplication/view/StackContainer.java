package com.lwy.myapplication.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author lwy 2019-11-30
 * @version v1.0.0
 * @name StackContainer
 * @description
 */
public class StackContainer extends ViewGroup {

    public static final float sFirstAnimateParam = 0.9f;
    public static final float sSecondAnimateParam = 1f;
    public static final float sScaleXAnimateParam = .02f;
    private float firstAnimatingParam = sFirstAnimateParam;
    private float secondAnimatingParam = sSecondAnimateParam;
    private float scaleXAnimatingParam = sScaleXAnimateParam;

    private int collapseCount = 3;

    private boolean isAnimating = false;
    private int status = COLLAPSE;
    public static final int EXPAND = 0;
    public static final int COLLAPSE = 1;
    private long animatingDuration = 400;
    private int totalHeight; // children总高度

    private Adapter adapter;
    private int[] heights;

    public Adapter getAdapter() {
        return adapter;
    }

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
        if (adapter != null) {
            adapter.setView(this);
            isAnimating = false;
            firstAnimatingParam = sFirstAnimateParam;
            secondAnimatingParam = sSecondAnimateParam;
            status = COLLAPSE;
            refreshViewList();
            requestLayout();//1  onMeasure   2  onLayout
        }
    }

    private void refreshViewList() {
        removeAllViews();
        if (adapter.getCount() > -1) {
            for (int i = 0; i < adapter.getCount(); i++) {
                View view = adapter.getView(i, null, this);
                addView(view, 0);
            }

        }
    }

    public int getStatus() {
        return status;
    }

    private void notifyChanged() {
        if (isAnimating)
            return;
        refreshViewList();
        requestLayout();
    }

    public StackContainer(Context context) {
        this(context, null);
    }

    public StackContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
//        this.viewList = new ArrayList<>();
    }

    public void expand() {
        if (adapter == null || isAnimating || status == EXPAND) {
            return;
        }
        isAnimating = true;
//        final ValueAnimator scaleValueAnimator = ValueAnimator.ofFloat(sScaleXAnimateParam, 0);
//        scaleValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator animation) {
//                scaleXAnimatingParam = (float) animation.getAnimatedValue();
//            }
//        });

        ValueAnimator firstValueAnimator = ValueAnimator.ofFloat(sFirstAnimateParam, 0);
        firstValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                scaleXAnimatingParam = sScaleXAnimateParam * (1 - animation.getAnimatedFraction());
                firstAnimatingParam = (float) animation.getAnimatedValue();
                requestLayout();
            }
        });
        firstValueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                status = EXPAND;
                isAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                isAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        firstValueAnimator.setDuration(animatingDuration);

        firstValueAnimator.start();
        if (getChildCount() > collapseCount) {
            ValueAnimator secondValueAnimator = ValueAnimator.ofFloat(sSecondAnimateParam, 0);
            secondValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    secondAnimatingParam = (float) animation.getAnimatedValue();
                    requestLayout();
                }
            });
            secondValueAnimator.setDuration(animatingDuration);
            secondValueAnimator.start();
        }
    }

    public void collapse() {
        if (adapter == null || isAnimating || status == COLLAPSE) {
            return;
        }
        isAnimating = true;

        ValueAnimator firstValueAnimator = ValueAnimator.ofFloat(0, sFirstAnimateParam);
        firstValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                firstAnimatingParam = (float) animation.getAnimatedValue();
                scaleXAnimatingParam = sScaleXAnimateParam * animation.getAnimatedFraction();
                requestLayout();
            }
        });
        firstValueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                status = COLLAPSE;
                isAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                isAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        firstValueAnimator.setDuration(animatingDuration);
        firstValueAnimator.start();
        if (getChildCount() > collapseCount) {
            ValueAnimator secondValueAnimator = ValueAnimator.ofFloat(0, sSecondAnimateParam);
            secondValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    secondAnimatingParam = (float) animation.getAnimatedValue();
                    requestLayout();
                }
            });
            secondValueAnimator.setDuration(animatingDuration);
            secondValueAnimator.start();
        }
    }

    private int getMaxWidth() {
        int count = getChildCount();
        int maxWidth = 0;
        for (int i = 0; i < count; i++) {
            int currentWidth = getChildAt(i).getMeasuredWidth();
            if (maxWidth < currentWidth) {
                maxWidth = currentWidth;
            }
        }
        return maxWidth;
    }

    private int getTotalHeight() {
        if (adapter == null || adapter.getCount() <= 0) {
            return 0;
        }
        int count = adapter.getCount();
        totalHeight = 0;
        int collapseTotalHeight = 0;  // 折叠时需要显示的view的总高度，当count > collapseCount才有意义
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            int childHeight = child.getMeasuredHeight();
            if (i < count - 1) {
                child.setScaleX(1 - scaleXAnimatingParam * (count - i));
            }
            totalHeight += childHeight;
            if (count > collapseCount && i >= count - collapseCount) {
                collapseTotalHeight += childHeight;
            }
        }
        if (count == 1) {
            return totalHeight;
        } else {
            int lastPositionChildHeight = getChildAt(0).getMeasuredHeight();
            int offset;   // view的实际距离顶部0的向上偏移量
            if (count <= collapseCount) {
                // < 收缩的最大个数
                offset = (int) (lastPositionChildHeight * firstAnimatingParam) * (count - 1);
            } else {
                offset = (int) (lastPositionChildHeight * secondAnimatingParam) * (count - 1);
                int lastCollapseChildHeight = getChildAt(count - collapseCount).getMeasuredHeight();
                int lastCollapseChildFactor = (int) (lastCollapseChildHeight * firstAnimatingParam) * (collapseCount - 1);
                int realH1 = totalHeight - offset;  // 最底部摆放的view距离顶部的距离
                int realH2 = collapseTotalHeight - lastCollapseChildFactor;  // 需要折叠显示的最底部的view距离顶部的距离 ，
                return Math.max(realH1, realH2);   // 即实际该容器组件的高度
            }
            return totalHeight - offset;   // 子组件总高度 - 最底部摆放的view的向上偏移量 即：实际该容器组件的高度
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        int top = 0;
        int left = 0;
        top = totalHeight;
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            int width = view.getMeasuredWidth();
            int height = view.getMeasuredHeight();
            int offset = 0;
            if (i == count - 1) {
                // 第一个
            } else {
                if (i >= count - collapseCount) {
                    // < 收缩的最大个数
                    offset = (int) (height * firstAnimatingParam) * (count - 1 - i);
                } else {
                    offset = (int) (height * secondAnimatingParam) * (count - 1 - i);
                }
            }
            top = top - height;
            view.layout(left, top - offset, left + width, top - offset + height);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

//        for (int i = 0; i < getChildCount()-1; i++) {
//            View child = getChildAt(i);
//            child.getla
//        }
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);


        if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.AT_MOST) {
            int groupWidth = getMaxWidth();
            int groupHeight = getTotalHeight();

            setMeasuredDimension(groupWidth, groupHeight);
        } else if (widthMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(getMaxWidth(), getTotalHeight());
        } else if (heightMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(width, getTotalHeight());
        } else
            setMeasuredDimension(width, getTotalHeight());
    }

    public static abstract class Adapter {
        private StackContainer view;

        public void setView(StackContainer view) {
            this.view = view;
        }

        public StackContainer getView() {
            return view;
        }

        public void notifyChanged() {
            if (view != null) {
                view.notifyChanged();
            }
        }

        public abstract View getView(int position, View convertView, ViewGroup parent);

//        View onBinderViewHodler(int position, View convertView, ViewGroup parent);

        //Item的类型
//        int getItemViewType(int row);
//        //Item的类型数量
//        int getViewTypeCount();
        public abstract int getCount();
    }


}
