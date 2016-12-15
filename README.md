# Drag-Sort-Layout
拖动排序ViewGroup
![](GIF.gif)

能够实现view的拖动排序功能，添加了点击事件的回调和位置变化的回调

----
用法：
1. xml中：
```
<com.infinite.dragsortlayout.DragSortLayout
        android:id="@+id/dragLayout"
        android:background="@android:color/holo_blue_bright"
        app:columeSize="3"
        app:stretchMode="columeWidht"
        app:horizontalSpacing="10dp"
        app:verticalSpacing="10dp"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content">
```
添加布局，里面添加子view

2. 在代码中：
设置点击事件
```
mLayout.setOnItemClickListener(new DragSortLayout.OnItemClickListener() {
            @Override
            public void onItemClick(View childView, int position) {
                if (childView instanceof TextView)
                Log.e("click",((TextView)childView).getText()+"   "+position);
            }
        });
```
//位置变化的回调
```
mLayout.setOnPositionChangedListener(new DragSortLayout.OnPositionChangedListener() {
            @Override
            public void onPositionChanged(View dragView, View targetView, int dragPosition, int targetPosition) {
                Log.e("postion","dragPosition="+dragPosition+"  targetPosition="+targetPosition);
            }
        });
```
3. xml参数解释：

  | 参数  | 含义  |
  | :----:  | :-----: |
  | horizontalSpacing | 水平间距  |
  | verticalSpacing | 垂直间距  |
  | columeSize  |  列数 |
  | stretchMode | 适应模式 适应父view大小或者维持自己的大小 |

  ---
  update：
  * 添加了滑动和fling;
  * 修改了滑动后点击或长按是，对应view不正确的bug;
  * 当view拖动到屏幕底端或者顶端时，布局自动滚动一段距离
