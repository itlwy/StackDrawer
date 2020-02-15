package com.lwy.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lwy.myapplication.view.StackLayout;

import java.util.ArrayList;
import java.util.List;

public class TestActivity extends AppCompatActivity implements View.OnClickListener {

    private StackLayout stackLayout;
    private StackLayout stackLayout2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        initViews();
        iniListener();
    }

    private void initViews() {
        stackLayout = findViewById(R.id.stacklayout);
        stackLayout2 = findViewById(R.id.stacklayout2);
        findViewById(R.id.sub_btn).setOnClickListener(this);
        findViewById(R.id.add_btn).setOnClickListener(this);
        initStackView();
        initStackView2();
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


    private List<String> generateList() {
        List<String> retList = new ArrayList();
        for (int i = 0; i < 20; i++) {
            retList.add("item : " + i);
        }
        return retList;
    }

    private StackLayout initStackView() {
        stackLayout.nick = "first stacklayout";
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
//        stackLayout.setCollapseGap(3);
        List<String> datas = generateList();
        stackLayout.setAdapter(new TestActivity.MyAdapter(datas));
        stackLayout.setStatus(StackLayout.EXPAND);

//        stackLayout.setLayoutParams(param);
        return stackLayout;
    }

    private void initStackView2() {
        stackLayout2.nick = "second stacklayout";
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        List<String> datas = generateList();
        stackLayout2.setAdapter(new TestActivity.MyAdapter(datas));
        stackLayout2.setStatus(StackLayout.EXPAND);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sub_btn:
//                ((MyAdapter) stackLayout.getAdapter()).getDatas().set(0, "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
//                stackLayout.getAdapter().notifyChanged();
                ((MyAdapter.CustomViewHolder) stackLayout.getAdapter().getViewHolderAtIndex(0)).test();
                break;
            case R.id.add_btn:
//                ((MyAdapter) stackLayout.getAdapter()).getDatas().set(0, "item : " + 0);
//                stackLayout.getAdapter().notifyChanged();
                break;
        }
    }


    class MyAdapter extends StackLayout.Adapter<TestActivity.MyAdapter.CustomViewHolder> {
        private List<String> datas;

        public MyAdapter(List<String> datas) {
            this.datas = datas;
        }

        public List<String> getDatas() {
            return datas;
        }

        @Override
        public TestActivity.MyAdapter.CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = View.inflate(TestActivity.this, viewType, null);
            ViewGroup.LayoutParams param = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(param);
            return new TestActivity.MyAdapter.CustomViewHolder(view, this);
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
            return this.datas.size();
        }

        class CustomViewHolder extends StackLayout.ViewHolder {

            private final View itemLLt;
            private final TextView tv;
            private final MyAdapter adapter;

            public CustomViewHolder(View itemView, MyAdapter adapter) {
                super(itemView);
                this.adapter = adapter;
                itemLLt = itemView.findViewById(R.id.item_llt);
                tv = itemView.findViewById(R.id.tv);
            }

            public void bindViews(final int position) {
//                if (position != 1)
                tv.setText(adapter.datas.get(position));
                tv.setBackgroundColor(Color.CYAN);
                itemLLt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        int height = adapter.getView().getMeasuredHeight();
//                        int height1 = adapter.getView().getHeight();
                        if (position == 0) {
                            adapter.getView().switchStatus();
                        } else {
                            Toast.makeText(TestActivity.this, "点击了" + position, Toast.LENGTH_LONG).show();
                        }

                    }
                });
//                if (position == 0) {
//                    itemLLt.setBackgroundColor(Color.BLUE);
//                } else if (position == 1) {
//                    itemLLt.setBackgroundColor(Color.YELLOW);
//                } else if (position == 2) {
//                    itemLLt.setBackgroundColor(Color.LTGRAY);
//                } else {
//                    itemLLt.setBackgroundColor(Color.WHITE);
//                }
            }

            public void test() {
                tv.setText("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            }

        }
    }
}
