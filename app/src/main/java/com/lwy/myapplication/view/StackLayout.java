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

import com.lwy.myapplication.base.IScrollListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author lwy 2019-11-30
 * @version v1.0.0
 * @name StackContainer
 * @description collapse layout
 */
public class StackLayout extends ViewGroup implements IScrollListener {

    public static final int EXPAND = 0;
    public static final int COLLAPSE = 1;
    public static final float sScaleXAnimateParam = .02f;

    public String nick = "_";
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

    // 容器垂直方向滚动的值
    private int containerScrollY;


    private Adapter<ViewHolder> adapter;
    private List<StackStatusListener> listenerSet;
    private ValueAnimator valueAnimator;
    private int containerHeight;

    private boolean needRelayout = true;
    // 标识是否需要触发重新计算容器内的child的大小
    private boolean needReMeasure = true;
    // 在父布局的top值
    private int topAtPosition;
    // 当前显示的view集合 中，最底部可见的索引
    private int showingEndIndex;
    // 当前显示的view集合 中，最顶部可见的索引
    private int showingTopIndex;
    //    private int measureWidth;
    private int measureHeight;
    private int measureWidth;

    private LinkedHashMap<Integer, ViewHolder> showingViewList = new LinkedHashMap<>();

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

    public void addListener(StackStatusListener listener) {
        if (listener != null && !listenerSet.contains(listener)) {
            listenerSet.add(listener);
        }
    }

    public void removeListener(StackStatusListener listener) {
        if (listener != null && !listenerSet.contains(listener)) {
            listenerSet.add(listener);
        }
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
            needRelayout = true;
            needReMeasure = true;
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
                    needRelayout = true;
                    needReMeasure = true;
                    for (StackStatusListener listener : listenerSet) {
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
                    for (StackStatusListener listener : listenerSet) {
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

                    for (StackStatusListener listener : listenerSet) {
                        // 1-ratio 是为了 把 1-0的变化 对外屏蔽，对外抛出都为0-1的状态值变化
//                        listener.onStatusChangedProgress(status == EXPAND ? 1 - ratio : ratio, status == EXPAND ? totalHeight : collapseStatusHeight, collapseStatusHeight, totalHeight);
                        listener.onStatusChangedEnd(status == EXPAND ? COLLAPSE : EXPAND, status);
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
    private HashMap<String, ViewHolder> viewHolderMap = new HashMap<>();

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
                viewHolderList.add(0, viewHolder);
                adapter.onBindViewHolder(viewHolder, i);
//                addView(viewHolder.itemView, 0);
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
        needReMeasure = true;
        needRelayout = true;
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
        listenerSet = new ArrayList<>();
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
        int count = viewHolderList.size();
        for (int i = 0; i < count; i++) {
            if (i < count - collapseCount) {
                if (isShow) {
                    viewHolderList.get(i).itemView.setVisibility(VISIBLE);
                } else {
                    viewHolderList.get(i).itemView.setVisibility(GONE);
                }
            } else {
                viewHolderList.get(i).itemView.setVisibility(VISIBLE);
            }
        }

    }

    private int getMaxWidth() {
        int count = viewHolderList.size();
        int maxWidth = 0;
        for (int i = 0; i < count; i++) {
            View view = viewHolderList.get(i).itemView;
            int currentWidth = view.getMeasuredWidth();
            if (view.getVisibility() == GONE) {
                continue;
            }
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
        int count = viewHolderList.size();
        totalHeight = 0;
        for (int i = 0; i < count; i++) {
            View child = viewHolderList.get(i).itemView;
            if (child.getVisibility() == GONE) {
                continue;
            }
            int childHeight = child.getMeasuredHeight();
            if (i < count - 1) {
                child.setScaleX(1 - scaleXAnimatingParam * (count - i));
//                float aa = 1 - scaleXAnimatingParam * (count - i);
//                System.out.println("============> index : " + i + " , " + aa);
            }
            totalHeight += childHeight;
        }
        if (count == 1) {
            totalHeight = totalHeight + getPaddingBottom() + getPaddingTop();
            return totalHeight;
        } else {
            int firstPositionChildHeight = viewHolderList.get(count - 1).itemView.getMeasuredHeight();
            collapseStatusHeight = firstPositionChildHeight + (collapseCount - 1) * collapseGap;
            int offset = (int) ((totalHeight - collapseStatusHeight) * ratio);  // view的实际距离顶部0的向上偏移量
            collapseStatusHeight += getPaddingTop();
            totalHeight = totalHeight + getPaddingBottom() + getPaddingTop();
            return totalHeight - offset;   // 子组件总高度 - 最底部摆放的view的向上偏移量 即：实际该容器组件的高度
        }
    }


    /**
     * 判断是否需要触发重新布局，主要根据顶部第一个 、 底部最后可见的view的 索引来判断是否发生改变
     *
     * @return
     */
    private boolean isShouldRelayout() {
        int outOfContainerAtTop = topAtPosition - containerScrollY;
        int diffTop = 0;
        if (outOfContainerAtTop < 0) {
            // 顶部滚动容器顶部的距离， 即不可见的高度
            diffTop = outOfContainerAtTop;
        }

        // 底部可见的点
        int visibleBottom = containerHeight - topAtPosition + containerScrollY;

        int lastHeight = 0;
        int top = 0;
        // 是否布局第一个可见元素
        boolean isFirstLayout = true;

//        System.out.println("nick :" + nick + "======> 滚动触发 topAtPosition : " + topAtPosition + " , containerHeight : " + containerHeight + ", containerScrollY: " + containerScrollY);

        for (int i = viewHolderList.size() - 1; i >= 0; i--) {
            ViewHolder viewHolder = viewHolderList.get(i);
            View view = viewHolder.itemView;

            if (view.getVisibility() == GONE) {
                continue;
            }

            int height = view.getMeasuredHeight();
            diffTop += height;
            top = top + lastHeight;

            if (diffTop < 0) {
                lastHeight = height;
                if (i - 1 >= 0) {
                    // 还可遍历  这里主要是为了在屏幕上方不可见区域多缓存一个view

                    int tempDiffTop = diffTop + viewHolderList.get(i - 1).itemView.getMeasuredHeight();
                    if (tempDiffTop < 0) {
//                        System.out.println("nick :" + nick + "=======> 滚动触发 顶部不可见部分，当前i ： " + i);
                        continue;
                    }
                } else {
//                    System.out.println("nick :" + nick + "=======> 滚动触发 顶部不可见部分，当前i ： " + i);
                    continue;
                }
            }

            if (top >= visibleBottom) {
                // 到达底部不可见部分
//                System.out.println("nick :" + nick + "=======> 滚动触发 底部不可见部分触发到 ：showingEndIndex = " + showingEndIndex + ", i = " + i);

                if (top - lastHeight <= visibleBottom) {
                    // 判断是否第一个底部不可见的元素   是 则 放行，因为要多缓存一个屏幕低下的view

                } else {
                    if (showingEndIndex != i) {
                        return true;
                    }
                    break;
                }

            }
            if (isFirstLayout) {
//                System.out.println("nick :" + nick + "=======> 滚动触发 顶部首项可见的！showingTopIndex = " + showingTopIndex + ", i = " + i);
                if (showingTopIndex != i) {
                    return true;
                }
                isFirstLayout = false;
            }
            lastHeight = height;
//            System.out.println("nick :" + nick + "=======> 滚动触发 正常遍历i = " + i);
        }
        return false;
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (needRelayout || changed) {

            removeAllViewsInLayout();

            if (adapter != null && adapter.getItemCount() > 0) {

                topAtPosition = t;
//                System.out.println("nick :" + nick + "======> topAtPosition : " + topAtPosition + " , containerHeight : " + containerHeight + ", containerScrollY: " + containerScrollY);
                int count = adapter.getItemCount();
                int top = 0;
                int left = 0;

                int outOfContainerAtTop = topAtPosition - containerScrollY;
                int diffTop = 0;
                if (outOfContainerAtTop < 0) {
                    // 顶部滚动容器顶部的距离， 即不可见的高度
                    diffTop = outOfContainerAtTop;
                }

                // 底部可见的点
                int visibleBottom = containerHeight - topAtPosition + containerScrollY;

                int lastHeight = 0;
                // 是否布局第一个可见元素
                boolean isFirstLayout = true;
                for (int i = count - 1; i >= 0; i--) {

                    View view = viewHolderList.get(i).itemView;

                    if (view.getVisibility() == GONE) {
                        continue;
                    }

                    int width = view.getMeasuredWidth();
                    int height = view.getMeasuredHeight();
                    diffTop += height;
//                    System.out.println("nick :" + nick + "=======> diffTop ：" + diffTop + " , visibleBottom : " + visibleBottom);
                    top = top + lastHeight;

                    if (diffTop < 0) {
                        // diffTop == 0 时 实际刚好此view刚好是屏幕上方最接近第一个可见的不可见元素 特殊放行
                        lastHeight = height;
                        if (i + 1 >= 0) {
                            // 还可遍历  这里主要是为了在屏幕上方不可见区域多缓存一个view

                            int tempDiffTop = diffTop + viewHolderList.get(i - 1).itemView.getMeasuredHeight();
                            if (tempDiffTop < 0) {
//                                System.out.println("nick :" + nick + "=======> 顶部不可见部分，当前i ： " + i);
                                continue;
                            }
                        } else {
//                            System.out.println("nick :" + nick + "=======> 顶部不可见部分，当前i ： " + i);
                            continue;
                        }
                    }
                    if (top >= visibleBottom) {
                        // 到达底部不可见部分
                        showingEndIndex = i;
                        if (top - lastHeight <= visibleBottom) {
                            // 判断是否第一个底部不可见的元素   是 则 放行，因为要多缓存一个屏幕低下的view

                        } else {
                            if (getChildCount() > 0 && getChildCount() < collapseCount) {
//                                System.out.println("nick :" + nick + "=======> 到达底部不可见部分 ,当前i ： " + i + ",但是仍小于collapseCount数量，放行");
                            } else {
//                                System.out.println("nick :" + nick + "=======> 到达底部不可见部分 ,当前i ： " + i);
                                break;
                            }
                        }
                    }
                    if (isFirstLayout) {
                        showingTopIndex = i;
                        isFirstLayout = false;
                    }
                    addView(view, 0);

//                    System.out.println("nick :" + nick + "=======> addView count : " + getChildCount());

                    int offset = 0;
                    // 这里的offset主要用于 折叠效果
                    if (i == count - 1) {

                    } else {
                        if (i >= count - collapseCount) {
                            if (top - collapseStatusHeight < 0) {
                                offset = (int) ((height + (count - collapseCount - i) * collapseGap) * ratio);
                            } else {
                                offset = (int) ((top - collapseStatusHeight + height) * ratio);
                            }
                        } else {
                            offset = (int) ((top - collapseStatusHeight + height) * ratio);
                        }
                    }
                    view.layout(left, top - offset, left + width, top - offset + height);
                    lastHeight = height;
                }
            }

        }

//        int paddingBottom = getPaddingBottom();
//        top = totalHeight - getPaddingBottom();
//        for (int i = 0; i < count; i++) {
//            View view = getChildAt(i);
//            int width = view.getMeasuredWidth();
//            int height = view.getMeasuredHeight();
//            int offset = 0;
//            top = top - height;
//            if (i == count - 1) {
//                // 第一个
//            } else {
//                if (i >= count - collapseCount) {
//                    // < 收缩的最大个数
//                    if (top - collapseStatusHeight < 0) {
////                        // TODO : 从上往下数 第2个view的特殊处理
//                        offset = (int) ((height + (count - collapseCount - i) * collapseGap) * ratio);
//                    } else {
//                        offset = (int) ((top - collapseStatusHeight + height) * ratio);
//                    }
//                } else {
//                    offset = (int) ((top - collapseStatusHeight + height) * ratio);
//                }
//            }
//            view.layout(left, top - offset, left + width, top - offset + height);
//        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        measureChildren(widthMeasureSpec, heightMeasureSpec);
//        this.widthMeasureSpec = widthMeasureSpec;
//        this.heightMeasureSpec = heightMeasureSpec;
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);

//        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
//        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (viewHolderList != null && viewHolderList.size() > 0) {
            if (needReMeasure) {
                for (int i = 0; i < viewHolderList.size(); i++) {
                    View child = viewHolderList.get(i).itemView;
                    measureChild(child, widthMeasureSpec, heightMeasureSpec);
                }
                if (widthMode == MeasureSpec.AT_MOST) {
                    measureWidth = getMaxWidth();
                    measureHeight = getTotalHeight();
                } else {
                    measureWidth = width;
                    measureHeight = getTotalHeight();
                }
                setMeasuredDimension(measureWidth, measureHeight);
            } else {
//                System.out.println("1111111 滚动触发 说明数据集没有发生变化，没必要重新测量");
                // 滚动触发 说明数据集没有发生变化，没必要重新测量
                setMeasuredDimension(measureWidth, measureHeight);
            }
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

    }

    @Override
    public void onScrollChanged(int scrollX, int scrollY) {
//        System.out.println("nick :" + nick +"=======> stacklayout onScrollChanged");
        containerScrollY = scrollY;
        if (/*status == EXPAND && */isShouldRelayout()) {
            needRelayout = true;
            needReMeasure = false;
            if (!isLayoutRequested()) {
                requestLayout();
            }
        }
    }

    @Override
    public void onVisibleSizeChanged(int visibleWidth, int visibleHeight) {
        containerHeight = visibleHeight;
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

        public int getHeight(int position) {
            return 0;
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
