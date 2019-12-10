package com.lwy.myapplication.view;


import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lwy 2019-11-30
 * @version v1.0.0
 * @name StackContainer
 * @description collapse layout
 */
public class StackLayout extends ViewGroup {

    public static final int EXPAND = 0;
    public static final int COLLAPSE = 1;
    public static final float sScaleXAnimateParam = .02f;

    /**
     * 横向缩放的比例
     */
    private float scaleXAnimatingParam = sScaleXAnimateParam;

    /**
     * 当前状态
     */
    private int status = COLLAPSE;

    /**
     * 折叠的间隙 单位 : dp
     */
    private int collapseGap;

    /**
     * 折叠的数量
     */
    private int collapseCount = 3;

    private boolean isAnimating = false;
    private long animatingDuration = 400;

    /**
     * children总高度
     */
    private int totalHeight;
    /**
     * 折叠状态的高度
     */
    private int collapseStatusHeight;
    /**
     * 折叠、展开的过渡比例  ： 1:折叠，0:展开
     */
    private float ratio = 1;

    private Adapter<ViewHolder> adapter;
    private StackStatusListener listener;
    private ValueAnimator valueAnimator;
//    private int screenWidth;
//    private int screenHeight;


    public void setStatus(int status) {
        if (status == EXPAND) {
            scaleXAnimatingParam = 0;
            ratio = 0;
        } else if (status == COLLAPSE) {
            scaleXAnimatingParam = sScaleXAnimateParam;
            ratio = 1;
        } else {
            return;
        }
        this.status = status;
//        requestLayout();
    }

    public boolean isAnimating() {
        return isAnimating;
    }

    public void setCollapseCount(int collapseCount) {
        this.collapseCount = collapseCount;
    }

    public long getAnimatingDuration() {
        return animatingDuration;
    }

    public void setAnimatingDuration(long animatingDuration) {
        this.animatingDuration = animatingDuration;
        valueAnimator = null;
    }

    public void setListener(StackStatusListener listener) {
        this.listener = listener;
    }

    public StackStatusListener getListener() {
        return listener;
    }

    /**
     * 首个View下面叠加的view显示出来的间隙
     *
     * @param collapseGap 单位:dp
     */
    public void setCollapseGap(int collapseGap) {
        this.collapseGap = collapseGap;
    }

    public Adapter getAdapter() {
        return adapter;
    }

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
        if (adapter != null) {
            adapter.setView(this);
            isAnimating = false;
            status = COLLAPSE;
            scaleXAnimatingParam = sScaleXAnimateParam;
            ratio = 1;
            valueAnimator = null;
            refreshViewList();
            requestLayout();//1  onMeasure   2  onLayout
        }
    }

    private ValueAnimator getValueAnimator() {
        if (valueAnimator == null) {
            valueAnimator = ValueAnimator.ofFloat(1, 0);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    ratio = (float) animation.getAnimatedValue();
                    if (status == EXPAND) {
                        // 当前处于展开状态  正在向 收起状态过渡
                        scaleXAnimatingParam = sScaleXAnimateParam * (1 - animation.getAnimatedFraction());
                    } else {
                        scaleXAnimatingParam = sScaleXAnimateParam * (1 - animation.getAnimatedFraction());
                    }
                    requestLayout();
                    if (listener != null) {
                        listener.onStatusChangedProgress(status == COLLAPSE ? 1 - ratio : ratio, getMeasuredHeight(), collapseStatusHeight, totalHeight);
                    }
                }
            });
            valueAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    if (status == COLLAPSE) {
                        setCollapseViewVisiable(true);
                    }
                    if (listener != null) {
                        listener.onStatusChangedStart(status, status == EXPAND ? COLLAPSE : EXPAND);
                    }
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (status == EXPAND) {
                        status = COLLAPSE;
                        setCollapseViewVisiable(false);
                    } else {
                        status = EXPAND;
                    }
                    isAnimating = false;
                    if (listener != null) {
                        listener.onStatusChangedEnd(status == EXPAND ? COLLAPSE : EXPAND, status);
                        // 1-ratio 是为了 把 1-0的变化 对外屏蔽，对外抛出都为0-1的状态值变化
//                        listener.onStatusChangedProgress(status == EXPAND ? 1 - ratio : ratio, status == EXPAND ? totalHeight : collapseStatusHeight, collapseStatusHeight, totalHeight);
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    isAnimating = false;
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            valueAnimator.setDuration(animatingDuration);
        }
        return valueAnimator;
    }

    private List<ViewHolder> viewHolderList;
    private Map<String, ViewHolder> viewHolderMap;

    /**
     * 刷新view进容器，将顺序进行倒转
     */
    private void refreshViewList() {
        removeAllViews();
        viewHolderList.clear();
        HashMap<String, ViewHolder> tempViewHolderMap = new HashMap<>();
        if (adapter.getItemCount() > -1) {
            for (int i = 0; i < adapter.getItemCount(); i++) {
                ViewHolder viewHolder;
                int type = adapter.getItemViewType(i);
                String key = type + "_" + i;
                if (viewHolderMap.containsKey(key)) {
                    viewHolder = viewHolderMap.get(key);
                } else {
                    viewHolder = adapter.onCreateViewHolder(this, type);
                }
                tempViewHolderMap.put(key, viewHolder);
                viewHolderList.add(viewHolder);
                adapter.onBindViewHolder(viewHolder, i);
                addView(viewHolder.itemView, 0);
            }
            viewHolderMap = tempViewHolderMap;
        }
    }

    /**
     * 获取当前的状态
     *
     * @return one of {@link #COLLAPSE}, {@link #EXPAND}.
     */
    public int getStatus() {
        return status;
    }

    /**
     * 通知数据源变化，由adapter调用
     */
    private void notifyChanged() {
        if (isAnimating)
            return;
        refreshViewList();
        requestLayout();
    }

    /**
     * 切换折叠、展开状态
     */
    public void switchStatus() {
        if (getStatus() == StackLayout.COLLAPSE) {
            expand();
        } else {
            collapse();
        }
    }

    public StackLayout(Context context) {
        this(context, null);
    }

    public StackLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StackLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        collapseGap = dp2px(8);
        viewHolderList = new ArrayList<>();
        viewHolderMap = new HashMap<>();
//        Point p = new Point();
//        ((Activity) context).getWindowManager().getDefaultDisplay().getSize(p);
//        screenWidth = p.x;
//        screenHeight = p.y;
    }

    /**
     * 展开
     */
    public void expand() {
        if (adapter == null || isAnimating || status == EXPAND) {
            return;
        }
        isAnimating = true;

        getValueAnimator().start();
    }

    /**
     * 折叠收起
     */
    public void collapse() {
        if (adapter == null || isAnimating || status == COLLAPSE) {
            return;
        }
        isAnimating = true;
        getValueAnimator().reverse();
    }

    /**
     * 切换折叠时不需要显示的View的可见性
     *
     * @param isShow
     */
    private void setCollapseViewVisiable(boolean isShow) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            if (i < count - collapseCount) {
                if (isShow) {
                    getChildAt(i).setVisibility(VISIBLE);
                } else {
                    getChildAt(i).setVisibility(GONE);
                }
            }
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
        if (adapter == null || adapter.getItemCount() <= 0) {
            return 0;
        }
        int count = adapter.getItemCount();
        totalHeight = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            int childHeight = child.getMeasuredHeight();
            if (i < count - 1) {
                child.setScaleX(1 - scaleXAnimatingParam * (count - i));
            }
            totalHeight += childHeight;
        }
        if (count == 1) {
            return totalHeight;
        } else {
            int firstPositionChildHeight = getChildAt(count - 1).getMeasuredHeight();
            collapseStatusHeight = firstPositionChildHeight + (collapseCount - 1) * collapseGap;
            int offset = (int) ((totalHeight - collapseStatusHeight) * ratio);  // view的实际距离顶部0的向上偏移量
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
            top = top - height;
            if (i == count - 1) {
                // 第一个
            } else {
                if (i >= count - collapseCount) {
                    // < 收缩的最大个数
                    if (top - collapseStatusHeight < 0) {
//                        // TODO : 从上往下数 第2个view的特殊处理
                        offset = (int) ((height + (count - collapseCount - i) * collapseGap) * ratio);
                    } else {
                        offset = (int) ((top - collapseStatusHeight + height) * ratio);
                    }
                } else {
                    offset = (int) ((top - collapseStatusHeight + height) * ratio);
                }
            }
            view.layout(left, top - offset, left + width, top - offset + height);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
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

    public static abstract class Adapter<VH extends ViewHolder> {
        public static final long NO_ID = -1;
        private StackLayout view;

        public void setView(StackLayout view) {
            this.view = view;
        }

        public StackLayout getView() {
            return view;
        }

        public void notifyChanged() {
            if (view != null) {
                view.notifyChanged();
            }
        }

        public abstract VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType);

        public abstract void onBindViewHolder(@NonNull VH holder, int position);

        public int getItemViewType(int position) {
            return 0;
        }

        public abstract int getItemCount();

        public long getItemId(int position) {
            return NO_ID;
        }

        /**
         * 发送消息给StackLayout的所有viewholder，可用于局部刷新
         *
         * @param message
         */
        public void sendMessage2ViewHolders(int type, Object... message) {
            getView().sendMessage2ViewHolders(type, message);
        }

        public ViewHolder getViewHolderAtIndex(int index) {
            return getView().getViewHolderAtIndex(index);
        }
    }

    public ViewHolder getViewHolderAtIndex(int index) {
        return viewHolderList.get(index);
    }

    private void sendMessage2ViewHolders(int type, Object... message) {
        if (viewHolderList != null) {
            for (int i = 0; i < viewHolderList.size(); i++) {
                viewHolderList.get(i).onMessageArrived(type, message);
            }
        }
    }

    public abstract static class ViewHolder {
        public final View itemView;

        public ViewHolder(View itemView) {
            if (itemView == null) {
                throw new IllegalArgumentException("itemView may not be null");
            }
            this.itemView = itemView;
        }

        public void onMessageArrived(int type, Object... message) {

        }

    }

    public interface StackStatusListener {
        /**
         * 状态切换开始回调
         *
         * @param currentStatus one of {@link #COLLAPSE}, {@link #EXPAND}.
         * @param nextStatus    one of {@link #COLLAPSE}, {@link #EXPAND}.
         */
        void onStatusChangedStart(int currentStatus, int nextStatus);

        /**
         * 状态切换结束回调
         *
         * @param currentStatus one of {@link #COLLAPSE}, {@link #EXPAND}.
         * @param nextStatus    one of {@link #COLLAPSE}, {@link #EXPAND}.
         */
        void onStatusChangedEnd(int currentStatus, int nextStatus);

        /**
         * 状态切换进度回调
         *
         * @param ratio                折叠、展开的过渡比例 : 1:折叠 - 0:展开
         * @param currentHeight        过渡过程中当前高度
         * @param collapseStatusHeight 完全折叠时的总高度
         * @param expandStatusHeight   完全展开时的总高度
         */
        void onStatusChangedProgress(float ratio, int currentHeight, int collapseStatusHeight, int expandStatusHeight);

    }

    /**
     * dp转px
     */
    public static int dp2px(float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpVal, Resources.getSystem().getDisplayMetrics());
    }

}
