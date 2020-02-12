package com.lwy.myapplication.base;

/**
 * @author lwy 2020-02-09
 * @version v1.0.0
 * @name IScrollListener
 * @description
 */
public interface IScrollListener {

    /**
     * 由父ScrollView等通知其（子）发生滚动
     *
     * @param scrollX       当前滚动量  对应 ScrollY
     * @param scrollY       当前滚动量  对应 ScrollX
     */
    void onScrollChanged(int scrollX, int scrollY);

    /**
     *
     * @param visibleWidth  滚动容器的可视宽度
     * @param visibleHeight 滚动容器的可视高度
     */
    void onVisibleSizeChanged(int visibleWidth, int visibleHeight);

}
