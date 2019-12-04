package com.lwy.myapplication;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lwy.myapplication.view.StackLayout;

public class MainActivity extends AppCompatActivity {

    private LinearLayout llt1;
    private StackLayout stackLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        llt1 = findViewById(R.id.llt1);
        stackLayout = initStackView();
        iniListener();
    }

    private void iniListener() {
        stackLayout.setListener(new StackLayout.StackStatusListener() {
            @Override
            public void onStatusChangedStart(int currentStatus, int nextStatus) {
                System.out.println("=============> onStatusChangedStart : currentStatus :" + currentStatus + ", nextStatus : " + nextStatus);
            }

            @Override
            public void onStatusChangedEnd(int currentStatus, int nextStatus) {
                System.out.println("=============> onStatusChangedEnd : currentStatus :" + currentStatus + ", nextStatus : " + nextStatus);
            }

            @Override
            public void onStatusChangedProgress(float ratio, int currentHeight, int totalHeight) {
                System.out.println("=============> onStatusChangedProgress :  ratio : " + ratio + " , currentHeight：" + currentHeight + ", totalHeight : " + totalHeight);
            }

        });
    }

    private StackLayout initStackView() {
        final StackLayout stackLayout = new StackLayout(this);
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
//        stackLayout.setCollapseGap(3);
        stackLayout.setAdapter(new MyAdapter());
//        stackLayout.setStatus(StackLayout.EXPAND);
        llt1.addView(stackLayout, param);
        return stackLayout;
    }


    class MyAdapter extends StackLayout.Adapter<MyAdapter.CustomViewHolder> {

        @Override
        public MyAdapter.CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = View.inflate(MainActivity.this, viewType, null);
            ViewGroup.LayoutParams param = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(param);
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyAdapter.CustomViewHolder holder, int position) {
            holder.bindViews(position);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 1) {
                return R.layout.item_big;
            } else {
                return R.layout.item;
            }
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
                        if (position == 0) {
                            stackLayout.switchStatus();
                        } else {
                            Toast.makeText(MainActivity.this, "点击了" + position, Toast.LENGTH_LONG).show();
                        }

                    }
                });
            }
        }
    }

}
