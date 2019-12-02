package com.lwy.myapplication;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lwy.myapplication.view.StackContainer;

public class MainActivity extends AppCompatActivity {

    private LinearLayout llt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        llt = findViewById(R.id.llt);
        initStackView();
        initStackView();

    }

    private void initStackView() {
        final StackContainer stackContainer = new StackContainer(this);
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
//        stackContainer.setBackgroundColor(Color.LTGRAY);
        llt.addView(stackContainer, param);
        stackContainer.setAdapter(new MyAdapter());
    }


    class MyAdapter extends StackContainer.Adapter {

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = View.inflate(MainActivity.this, R.layout.item, null);
            View itemLLt = view.findViewById(R.id.item_llt);
            TextView tv = view.findViewById(R.id.tv);
            tv.setText("item : " + position);
            itemLLt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (position == 0) {
                        if (getView().getStatus() == StackContainer.COLLAPSE) {
                            getView().expand();
                        } else {
                            getView().collapse();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "点击了" + position, Toast.LENGTH_LONG).show();
                    }

                }
            });

            ViewGroup.LayoutParams param = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(param);
            return view;
        }

        @Override
        public int getCount() {
            return 7;
        }
    }

}
