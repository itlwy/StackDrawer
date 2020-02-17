package com.lwy.stacklib.view;


import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import com.lwy.stacklib.base.IScrollListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lwy 2019-11-30
 * @version v1.0.0
 * @name StackContainer
 * @description 折叠展开容器组件，可结合 StackScrollView 实现recycleview的缓存viewholder机制
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
     * 折叠时展示的数量
     */
    private int collapseCount = 3;

    private boolean isAnimating = false;
    private long animatingDuration = 400; // 400

    /**
     * children总高度
     */
    private int totalHeight;
    /**
     * 完全折叠状态的总高度
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
    /**
     * 父容器的可视高度
     */
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

    // 每次测量后的高度
    private int measureHeight;

    private int widthMeasureSpec = MeasureSpec.makeMeasureSpec(1 << 30 - 1, MeasureSpec.AT_MOST);
    private int heightMeasureSpec = MeasureSpec.makeMeasureSpec(1 << 30 - 1, MeasureSpec.AT_MOST);

    // 当前正在展示的child view集合
    private SparseArray<ViewHolder> showingViewList = new SparseArray<>();
    // viewholder的缓存池
    private Map<Integer, List<ViewHolder>> cacheViewHolder = new HashMap<>();
    // 所有item的高度数组
    private int[] heights;
    // 所有item的宽度数组
    private int[] widths;

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
            cacheViewHolder.clear();
            showingViewList.clear();
            caculatorSize();
            requestLayout();//1  onMeasure   2  onLayout
        }
    }

    /**
     * 折叠展开的动画
     *
     * @return
     */
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

    /**
     * 预计算StackLayout的总高度（展开的）
     */
    private void caculatorSize() {
        if (adapter != null) {
            heights = new int[adapter.getItemCount()];
            widths = new int[adapter.getItemCount()];
            totalHeight = 0;
            for (int i = 0; i < adapter.getItemCount(); i++) {
                if (adapter.getSize(i) != null && adapter.getSize(i).length == 2) {
                    widths[i] = adapter.getSize(i)[0];
                    heights[i] = adapter.getSize(i)[1];
                } else {
                    int type = adapter.getItemViewType(i);
                    ViewHolder viewHolder = adapter.onCreateViewHolder(this, type);
                    adapter.onBindViewHolder(viewHolder, i);
                    if (viewHolder.itemView.getVisibility() == GONE) {
                        continue;
                    }
                    viewHolder.itemView.measure(this.widthMeasureSpec, this.heightMeasureSpec);
                    heights[i] = viewHolder.itemView.getMeasuredHeight();
                    widths[i] = viewHolder.itemView.getMeasuredWidth();
                }
                totalHeight += heights[i];
            }
        }
    }

    /**
     * 优先从cache中取viewHolder，没有则调用adapter生成
     *
     * @param type
     * @return
     */
    private ViewHolder obtainViewHolder(int type) {
        List<ViewHolder> cacheList = cacheViewHolder.get(type);
        if (cacheList == null) {
            cacheList = new ArrayList<>();
            cacheViewHolder.put(type, cacheList);
        }
        if (cacheList.size() > 0) {
            return cacheList.remove(cacheList.size() - 1);
        } else {
            ViewHolder viewholder = adapter.onCreateViewHolder(this, type);
//            measureChild(viewholder.itemView, this.widthMeasureSpec, this.heightMeasureSpec);
            return viewholder;
        }
    }

    /**
     * 缓存不可见的viewHolder
     *
     * @param viewHolder
     * @param type
     */
    private void cacheViewHolder(ViewHolder viewHolder, int type) {
        List<ViewHolder> cacheList = cacheViewHolder.get(type);
        if (cacheList == null) {
            cacheList = new ArrayList<>();
            cacheViewHolder.put(type, cacheList);
        }
        cacheList.add(viewHolder);
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
        caculatorSize();
        showingViewList.clear();
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
        cacheViewHolder = new HashMap<>();
        listenerSet = new ArrayList<>();
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
        int count = showingViewList.size();
        for (int i = 0; i < count; i++) {
            if (i >= collapseCount) {
                if (isShow) {
                    showingViewList.get(i).itemView.setVisibility(VISIBLE);
                } else {
                    showingViewList.get(i).itemView.setVisibility(GONE);
                }
            } else {
                showingViewList.get(i).itemView.setVisibility(VISIBLE);
            }
        }

    }

    /**
     * 判断是否需要触发重新布局，主要根据顶部第一个 、 底部最后可见的view的 索引来判断是否发生改变
     *
     * @return
     */
    private boolean isShouldRelayout() {
        int visibleTopPosition = topAtPosition - containerScrollY;
        int outScreenOfTop = 0;
        if (visibleTopPosition < 0) {
            // 顶部滚动容器顶部的距离， 即不可见的高度
            outScreenOfTop = visibleTopPosition;
        }

        // 底部可见的边界
        int visibleBottom = containerHeight - topAtPosition + containerScrollY;

        int lastHeight = 0;
        int top = 0;

        boolean isFirstLayout = true;

//        System.out.println("nick :" + nick + "======> 滚动触发 topAtPosition : " + topAtPosition + " , containerHeight : " + containerHeight + ", containerScrollY: " + containerScrollY);

        for (int i = adapter.getItemCount() - 1; i >= 0; i--) {
            ViewHolder viewHolder = showingViewList.get(i);
            if (viewHolder != null) {

                View view = viewHolder.itemView;

                if (view.getVisibility() == GONE) {
                    if (status == COLLAPSE) {
                        break;
                    }
                    continue;
                }
            }
            int height = heights[i];
            outScreenOfTop += height;
            top = top + lastHeight;

            if (outScreenOfTop < 0) {
                lastHeight = height;
                if (i + 1 < heights.length) {
                    // 还可遍历  这里主要是为了在屏幕上方不可见区域多缓存一个view
                    int tempDiffTop = outScreenOfTop + heights[i + 1];
                    if (tempDiffTop < 0) {
                        continue;
                    }
                } else {
                    continue;
                }
            }

            if (top >= visibleBottom) {
                // 到达底部不可见部分
                if (top - lastHeight <= visibleBottom) {
                    // 判断是否第一个底部不可见的元素   是 则 放行，因为要多缓存一个屏幕低下的view

                } else {
                    if (showingEndIndex != i) {
//                    当前需要展示的child下边界发生变化
                        return true;
                    }
                    break;
                }

            }
            if (isFirstLayout) {
//                System.out.println("nick :" + nick + "=======> 滚动触发 顶部首项可见的！showingTopIndex = " + showingTopIndex + ", i = " + i);
                if (showingTopIndex != i) {
//                    当前需要展示的child上边界发生变化
                    return true;
                }
                isFirstLayout = false;
            }
            lastHeight = height;
        }
        return false;
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (needRelayout || changed) {

            removeAllViews();

            if (adapter != null && adapter.getItemCount() > 0) {

                topAtPosition = t;
//                System.out.println("nick :" + nick + "======> topAtPosition : " + topAtPosition + " , containerHeight : " + containerHeight + ", containerScrollY: " + containerScrollY);
                int count = adapter.getItemCount();
                int top = 0;
                int left = 0;

                int visibleTopPosition = topAtPosition - containerScrollY; // 滚动之后，view在父容器中实际的top值 ，当其等于0则说明该view滚动到达容器顶部了
                int outScreenOfTop = 0;  // 此view移出顶部的距离
                if (visibleTopPosition < 0) {
                    // 此时 此View的顶部已经有部分开始移出上方屏幕 outScreenOfTop
                    outScreenOfTop = visibleTopPosition;
                }

                // 底部可见的边界
                int visibleBottom = containerHeight - topAtPosition + containerScrollY;

                // 上一个child的高度
                int lastHeight = 0;
                // 是否布局时的第一次遍历
                boolean isFirstLayout = true;
                for (int i = 0; i < count; i++) {
                    if (!isAnimating && status == COLLAPSE && getChildCount() == collapseCount) {
                        // 收起状态且 已经达到设置的折叠数，则不需要继续遍历了
                        break;
                    }
                    ViewHolder viewHolder = showingViewList.get(i);
                    if (viewHolder != null) {

//                        System.out.println("nick :" + nick + "=======> showingViewList.get(i) - i：" + i + " visiable: " + (viewHolder.itemView.getVisibility() == VISIBLE ? "VIsiable" : "GOne"));
                        View view = viewHolder.itemView;

                        if (view.getVisibility() == GONE) {
//                            System.out.println("nick :" + nick + "=======> view.getVisibility() == GONE ：");
                            continue;
                        }
                    }
                    int height = heights[i];
                    outScreenOfTop += height;
//                    System.out.println("nick :" + nick + "=======> outScreenOfTop ：" + outScreenOfTop + " , visibleBottom : " + visibleBottom);
                    top = top + lastHeight;

                    if (outScreenOfTop < 0) {
                        // < 0 说明此时遍历的view还在此StackLayout顶部不可见
                        lastHeight = height;
                        if (i + 1 < heights.length) {
                            // 还可遍历  这里主要是为了在屏幕上方不可见区域多缓存一个view
                            int tempDiffTop = outScreenOfTop + heights[i + 1];
                            if (tempDiffTop < 0) {
                                // 仍然是不可见的
//                                System.out.println("nick :" + nick + "=======> 顶部不可见部分，当前i ： " + i);
                                if (showingViewList.indexOfKey(i) >= 0) {
                                    // 如在当前显示列表则移出 入 cache
                                    ViewHolder gViewHolder = showingViewList.get(i);
                                    cacheViewHolder(gViewHolder, adapter.getItemViewType(i));
                                    showingViewList.remove(i);
                                }
                                continue;
                            }
                        } else {
                            // 已经没有更多的view了，说明整个StackLayout都是不可见的
//                            System.out.println("nick :" + nick + "=======> 顶部不可见部分，当前i ： " + i);
                            if (showingViewList.indexOfKey(i) >= 0) {
                                ViewHolder gViewHolder = showingViewList.get(i);
                                cacheViewHolder(gViewHolder, adapter.getItemViewType(i));
                                showingViewList.remove(i);
                            }
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
                                if (showingViewList.indexOfKey(i) >= 0) {
                                    ViewHolder gViewHolder = showingViewList.get(i);
                                    cacheViewHolder(gViewHolder, adapter.getItemViewType(i));
                                    showingViewList.remove(i);
                                }
                                continue;
                            }
                        }
                    }
                    if (isFirstLayout) {
                        showingTopIndex = i;
                        isFirstLayout = false;
                    }
                    if (viewHolder == null) {
//                        System.out.println("nick :" + nick + "=======> viewHolder == null i：" + i);
                        viewHolder = obtainViewHolder(adapter.getItemViewType(i));
                        adapter.onBindViewHolder(viewHolder, i);
                        if (viewHolder.itemView.getVisibility() == GONE) {
//                            System.out.println("nick :" + nick + "=======>viewHolder == null view.getVisibility() == GONE ：");
                            continue;
                        }
                    }

                    showingViewList.put(i, viewHolder);
//                    通过往头插入view以实现先插入的位于最后，即view层级的顶部
                    addView(viewHolder.itemView, 0);
                    measureChild(viewHolder.itemView, this.widthMeasureSpec, this.heightMeasureSpec);
                    int width = viewHolder.itemView.getMeasuredWidth();
//                    System.out.println("nick :" + nick + "=======> addView count : " + getChildCount());
                    int offset = 0;
                    // 这里的offset主要用于实现折叠效果的layout偏移量
                    if (i > 0) {
                        if (i < collapseCount) {
                            // 完全收起时，藏在第一个view 下面的
                            if (top - collapseStatusHeight < 0) {
                                // 当此时要布局的view的top在完全折叠状态下的高度里面时
                                offset = (int) ((height - (collapseCount - i - 1) * collapseGap) * ratio);
                            } else {
                                // 当此时要布局的view的top在完全折叠状态下的高度外时
                                offset = (int) ((top - collapseStatusHeight + height) * ratio);
                            }
                        } else {
                            offset = (int) ((top - collapseStatusHeight + height) * ratio);
                        }
                    }
//                    对正常布局的top、bottom进行offset的偏移 来产生收起、展开效果
                    viewHolder.itemView.layout(left, top - offset, left + width, top - offset + height);
                    lastHeight = height;
                }
                checkItemScaleX();  // 检查 有无 复用导致的 X缩放问题
            }

        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        measureChildren(widthMeasureSpec, heightMeasureSpec);
        this.widthMeasureSpec = widthMeasureSpec;
        this.heightMeasureSpec = heightMeasureSpec;
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
//        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
//        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (adapter != null && adapter.getItemCount() > 0) {
            if (needReMeasure) {
                // needReMeasure 用于屏蔽滚动发生后 触发的requestLayout （因为这时是不需要重新计算大小的）
//                System.out.println("1111111 滚动触发 需要重新测量");
                int maxHeight = 0;
                if (isAnimating) {
                    checkItemScaleX();
                }
                checkShowingViewsSize();
                if (adapter.getItemCount() == 1) {
                    // 数量一个 则不需要考虑收缩、展开的问题
                    totalHeight = totalHeight + getPaddingBottom() + getPaddingTop();
                    maxHeight = totalHeight;
                } else {
                    // 数量大于1 时 需要考虑收缩、展开时的总高度计算问题
                    int firstPositionChildHeight = heights[0];
                    collapseStatusHeight = firstPositionChildHeight + (collapseCount - 1) * collapseGap;
                    int offset = (int) ((totalHeight - collapseStatusHeight) * ratio);  // 完全收起和完全展开 2种情况下的 高度差
                    collapseStatusHeight += getPaddingTop();
                    totalHeight = totalHeight + getPaddingBottom() + getPaddingTop();
                    maxHeight = totalHeight - offset;   // 子组件总高度 - 最底部摆放的view的向上偏移量 即：实际该容器组件的当前高度
                }
                if (widthMode == MeasureSpec.AT_MOST) {
                    measureHeight = maxHeight;
                } else {
                    measureHeight = maxHeight;
                }
                setMeasuredDimension(width, measureHeight);
            } else {
//                System.out.println("1111111 滚动触发 说明数据集没有发生变化，没必要重新测量");
                // 滚动触发 说明数据集没有发生变化，没必要重新测量
                needReMeasure = true;
                setMeasuredDimension(width, measureHeight);
            }
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

    }

    /**
     * 用于防止子view 请求父改变大小时，重新检查，更新对应的Heighs
     * 以生成新的totalHeight（因为totalHeight是开始根据数据集来触发计算好的）
     */
    private void checkShowingViewsSize() {
        if (showingViewList != null && showingViewList.size() > 0) {
            for (int i = 0; i < showingViewList.size(); i++) {
                int key = showingViewList.keyAt(i);
                ViewHolder viewHolder = showingViewList.get(key);
                measureChild(viewHolder.itemView, this.widthMeasureSpec, this.heightMeasureSpec);
                if (heights[key] != viewHolder.itemView.getMeasuredHeight()) {
                    heights[key] = viewHolder.itemView.getMeasuredHeight();
                }
                if (widths[key] != viewHolder.itemView.getMeasuredWidth()) {
                    widths[key] = viewHolder.itemView.getMeasuredWidth();
                }
            }
        }
    }

    /**
     * 用于收起、展开动画时 计算每个item横向的缩放量
     */
    private void checkItemScaleX() {
        if (showingViewList != null && showingViewList.size() > 0) {
            for (int i = 0; i < showingViewList.size(); i++) {
                ViewHolder viewHolder = showingViewList.get(i);
                if (viewHolder != null) {
                    View child = viewHolder.itemView;
                    child.setScaleX(1 - scaleXAnimatingParam * i);
                }
            }
        }
    }

    @Override
    public void onScrollChanged(int scrollX, int scrollY) {
//        System.out.println("nick :" + nick +"=======> stacklayout onScrollChanged");
        containerScrollY = scrollY;
        if (isShouldRelayout()) {
            needRelayout = true;
            needReMeasure = false;
            requestLayout();
        }
    }

    @Override
    public void onVisibleSizeChanged(int visibleWidth, int visibleHeight) {
        containerHeight = visibleHeight;
    }

    /**
     * 根据index获取当前可视范围的ViewHolder
     *
     * @param index
     * @return 非可视范围的index获取会返回null
     */
    public ViewHolder getViewHolderAtIndex(int index) {
//        return viewHolderList.get(index);
        return showingViewList.get(index);
    }

    @Deprecated
    private void sendMessage2ViewHolders(int type, Object... message) {
        if (showingViewList != null) {
            for (int i = 0; i < showingViewList.size(); i++) {
                showingViewList.get(i).onMessageArrived(type, message);
            }
        }
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

        public int[] getSize(int position) {
            return null;
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

    public abstract static class ViewHolder {
        public final View itemView;

        public ViewHolder(View itemView) {
            if (itemView == null) {
                throw new IllegalArgumentException("itemView may not be null");
            }
            this.itemView = itemView;
        }

        @Deprecated
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
