package com.lwy.myapplication;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lwy.myapplication.view.StackLayout;

public class TestActivity extends AppCompatActivity {

    private StackLayout stackLayout;
    private FrameLayout framelayout;
//    private int secondExpandAddedPadding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        framelayout = findViewById(R.id.framelayout);
        stackLayout = findViewById(R.id.stacklayout);
//        secondExpandAddedPadding = StackLayout.dp2px(30);
        initStackView();
        iniListener();
    }

    private void iniListener() {
//        stackLayout.addListener(new StackLayout.StackStatusListener() {
//            int nextStatus;
//            int lastDistance = 0;  // 保存上一次的移动距离差
//            float lastRatio = 0;
//            int collectedGap = 0;
//
//            @Override
//            public void onStatusChangedStart(int currentStatus, int nextStatus) {
////                System.out.println("=============> onStatusChangedStart : currentStatus :" + currentStatus + ", nextStatus : " + nextStatus + ", lastRatio : " + lastRatio);
//                this.nextStatus = nextStatus;
//            }
//
//            @Override
//            public void onStatusChangedEnd(int currentStatus, int nextStatus) {
////                System.out.println("=============> onStatusChangedEnd : currentStatus :" + currentStatus + ", nextStatus : " + nextStatus);
//                // 下面是为了做float 转int 后精度损失的补偿
//                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) framelayout.getLayoutParams();
//                if (nextStatus == StackLayout.EXPAND) {
//                    params.height = params.height - collectedGap + secondExpandAddedPadding;
//                } else {
//                    params.height = params.height - collectedGap - secondExpandAddedPadding;
//                }
//                framelayout.setLayoutParams(params);
//
//                lastDistance = 0;
//                lastRatio = 0;
//                collectedGap = 0;
//            }
//
//            @Override
//            public void onStatusChangedProgress(float ratio, int currentHeight, int collapseStatusHeight, int expandStatusHeight) {
////                System.out.println("=============> onStatusChangedProgress : currentHeight :" + currentHeight + ", collapseStatusHeight : " + collapseStatusHeight + " , expandStatusHeight : " + expandStatusHeight);
//                int tempDistance;
//                int tempGap = (int) ((ratio - lastRatio) * secondExpandAddedPadding);
//                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) framelayout.getLayoutParams();
//                if (nextStatus == StackLayout.EXPAND) {
//                    tempDistance = currentHeight - collapseStatusHeight;
//                } else {
//                    tempDistance = currentHeight - expandStatusHeight;
//                    tempGap = -tempGap;
//                }
//                params.height = params.height + tempDistance - lastDistance;  // -lastDistance是为了把上一次处理过的距离差减掉
//                params.height += tempGap;
//
//                collectedGap += tempGap;
//                lastDistance = tempDistance;
//                lastRatio = ratio;
//
//                framelayout.setLayoutParams(params);
//
//            }
//
//
//        });
    }


    private StackLayout initStackView() {
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
//        stackLayout.setCollapseGap(3);
        stackLayout.setAdapter(new TestActivity.MyAdapter());
//        stackLayout.setLayoutParams(param);
//        stackLayout.setStatus(StackLayout.EXPAND);
        return stackLayout;
    }


    class MyAdapter extends StackLayout.Adapter<TestActivity.MyAdapter.CustomViewHolder> {

        @Override
        public TestActivity.MyAdapter.CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = View.inflate(TestActivity.this, viewType, null);
            ViewGroup.LayoutParams param = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(param);
            return new TestActivity.MyAdapter.CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TestActivity.MyAdapter.CustomViewHolder holder, int position) {
            holder.bindViews(position);
        }

        @Override
        public int getItemViewType(int position) {
//            if (position == 1) {
//                return R.layout.item_big;
//            } else {
            return R.layout.item;
//            }
        }

        @Override
        public int getItemCount() {
            return 9;
        }

        class CustomViewHolder extends StackLayout.ViewHolder {

            private final View itemLLt;
            private final TextView tv;

            public CustomViewHolder(View itemView) {
                super(itemView);
                itemLLt = itemView.findViewById(R.id.item_llt);
                tv = itemView.findViewById(R.id.tv);
            }

            public void bindViews(final int position) {
                if (position != 1)
                    tv.setText("item : " + position);
                itemLLt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int height = stackLayout.getMeasuredHeight();
                        int height1 = stackLayout.getHeight();
                        if (position == 0) {
                            stackLayout.switchStatus();
                        } else {
                            Toast.makeText(TestActivity.this, "点击了" + position, Toast.LENGTH_LONG).show();
                        }

                    }
                });
            }
        }
    }
}
