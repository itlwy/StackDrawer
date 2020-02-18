# 仿IOS通知栏折叠展开组件

一款实现仿IOS通知栏的折叠/展开效果的组件，欢迎Star，欢迎Fork，谢谢～

## 功能介绍

- [x] 折叠、展开动效
- [x] 支持自定义折叠数量
- [x] 滚动缓存复用View机制
- [x] 支持多类型item布局
- [ ] 局部刷新
- [ ] item动画

## 效果

<img src="https://raw.githubusercontent.com/itlwy/StackDrawer/master/pictures/%E6%95%88%E6%9E%9C%E5%9B%BE1.gif" height="600" width="300"/>

## 使用方式

### 1. 如何引入

- **Gradle依赖引入**

  #### step 1

  Add the JitPack repository to your build file

  ```
  	allprojects {
  			repositories {
  				...
  				maven { url 'https://jitpack.io' }
  			}
  		}
  ```

  #### Step 2

  Add the dependency

  ```
  dependencies {
  	         implementation 'com.github.itlwy:StackDrawer:v1.0.0'
  	}
  ```

### 2. xml布局引入

```
  <com.lwy.stacklib.view.StackLayout
                android:id="@+id/stacklayout"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"/>
```

### 3. 初始化Adapter

这里完全仿RecycleView模式进行

- 定义Adapter

```
class MyAdapter extends StackLayout.Adapter<NestingStackActivity.MyAdapter.CustomViewHolder> {
        private List<String> datas;

		...

        @Override
        public NestingStackActivity.MyAdapter.CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = View.inflate(NestingStackActivity.this, viewType, null);
            ViewGroup.LayoutParams param = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(param);
            return new NestingStackActivity.MyAdapter.CustomViewHolder(view, this);
        }

        @Override
        public void onBindViewHolder(@NonNull NestingStackActivity.MyAdapter.CustomViewHolder holder, int position) {
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
    }
```

- 定义ViewHolder

```
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
                tv.setText(adapter.datas.get(position));
                itemLLt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (position == 0) {
                            adapter.getView().switchStatus();
                        } else {
                            Toast.makeText(NestingStackActivity.this, "点击了" + position, Toast.LENGTH_LONG).show();
                        }

                    }
                });
            }

        }
```

- 使用

```
 stackLayout = findViewById(R.id.stacklayout);
    stackLayout.nick = "first stacklayout";
//        stackLayout.setCollapseGap(3);
    List<String> datas = generateList();
    stackLayout.setAdapter(new NestingStackActivity.MyAdapter(datas));
    stackLayout.setStatus(StackLayout.EXPAND);
```

### 4. 缓存复用

如果数据量大而需要滚动，直接将StackLayout包裹进ScrollView等是OK的，但是要想和RecycleView一样可以把滚出屏幕不可见的View回收再利用以保持同一时间有限数量View来优化效果，可以配合`StackScrollView`这一滚动组件实现，改变后的布局

```
<com.lwy.stacklib.view.StackScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical">

            <com.lwy.stacklib.view.StackLayout
                android:id="@+id/stacklayout"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"/>

            <com.lwy.stacklib.view.StackLayout
                android:id="@+id/stacklayout2"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"/>

        </LinearLayout>


    </com.lwy.stacklib.view.StackScrollView>
```

如上，`StackScrollView`是支持里面有多个`StackLayout`的，同时也不需要是直接子类，可以是子孙；事实上，你只需要用`StackScrollView`将`StackLayout`包裹起来即可实现缓存复用，不需要其他操作

![StackLayout-view层级图](https://raw.githubusercontent.com/itlwy/StackDrawer/master/picturesStackLayout-view层级图)

如上，折叠收起后只会有折叠数量的view渲染，展开后也只会屏幕可见区域范围的view会渲染，当发生滑动会把移出的view移出至缓存，需要进入屏幕的从缓存中取，这样就可以解决大量view的内存和滑动不流畅问题了

## 实现思路

### 折叠效果

由于要实现的折叠效果是仿IOS通知栏的效果，即：收起时，下方的卡片需要往上方卡片的下面塞进去的效果

1. 自定义ViewGroup重写onLayout方法，从上往下布置child时，增加一个偏移量控制因子，来使得每个卡片往上偏移

2. 在实现下方卡片往上方卡片偏移后，需要下方的VIew层级上要在上方的View的下一层，这样才能塞进去的效果

实现上述2点也就完成了预期效果，重要的代码如下

```
@Override
protected void onLayout(boolean changed, int l, int t, int r, int b) {
	...
	for (int i = 0; i < count; i++) {
		// obtainViewHolder(...)
		addView(viewHolder.itemView, 0);  // 关键代码1 通过往头插入view以实现先插入的位于最后，即view层级的顶部
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
        viewHolder.itemView.layout(left, top - offset, left + width, top - offset + height);  // 关键代码2 对正常布局的top、bottom进行offset的偏移 来产生收起、展开效果
	}
	...
}
```

### 支撑大量View的缓存复用机制

简单起见，想直接利用ScrollView的滚动能力，但是其无像RecycleView的高效复用能力，所以直接对其进行扩展，核心是将其可视高度、滚动的距离这2个变量传递给被包裹的StackLayout，StackLayout再根据其在ScrollView的top位置，也即`containerScrollY`、`containerHeight`、`topAtPosition`这三个变量，在onLayout环节进行布局时判断是否滚出不可见

<img src="https://raw.githubusercontent.com/itlwy/StackDrawer/master/pictures/StackLayout缓存思路1.png" height="600" width="800"/>

​								图1 - 初始状态

<img src="https://raw.githubusercontent.com/itlwy/StackDrawer/master/pictures/StackLayout缓存思路2.png" height="600" width="1024"/>

​								图2 - 向上滚动状态

## 依赖

- implementation 'com.android.support:appcompat-v7:28.0.0'

## License

   	Copyright 2020 lwy

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
