package com.infinite.dragsortlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Vibrator;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

/**
 * Created by inf on 2016/12/8.
 */

public class DragSortLayout extends ViewGroup{

    private static final int LONG_CLICK_MODE_TIME=300;
    private static final int DEFAULT_COLUME_SIZE=3;
    private static final float DEFAULT_VERTICAL_SPACING=20;
    private static final float DEFAULT_HORIZONTAL_SPACING=20;
    private static final int STRETCH_MODE_COLUME_WIDTH=0;
    private static final int STRETCH_MODE_NONE=1;
    private int mColumeSize=DEFAULT_COLUME_SIZE;
    private float mVerticalSpacing=DEFAULT_VERTICAL_SPACING;
    private float mHorizontalSpacing=DEFAULT_HORIZONTAL_SPACING;
    private int mStretchMode=STRETCH_MODE_NONE;

    private ViewDragHelper mDragHelper;

    public DragSortLayout(Context context) {
        this(context,null);
    }

    public DragSortLayout(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public DragSortLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray ta=context.obtainStyledAttributes(attrs, R.styleable.DragSortLayout, 0, defStyleAttr);
        mColumeSize=ta.getInteger(R.styleable.DragSortLayout_columeSize,DEFAULT_COLUME_SIZE);
        mHorizontalSpacing=ta.getDimension(R.styleable.DragSortLayout_horizontalSpacing,DEFAULT_HORIZONTAL_SPACING);
        mVerticalSpacing=ta.getDimension(R.styleable.DragSortLayout_horizontalSpacing,DEFAULT_VERTICAL_SPACING);
        mStretchMode=ta.getInt(R.styleable.DragSortLayout_stretchMode,STRETCH_MODE_NONE);
        ta.recycle();

        mDragHelper=ViewDragHelper.create(this, 1f,new ViewDragHelper.Callback() {
            @Override
            public boolean tryCaptureView(View child, int pointerId) {
                child.setAlpha(0.5f);
                return true;
            }

            @Override
            public int clampViewPositionHorizontal(View child, int left, int dx) {

                int leftOffset=getPaddingLeft();
                int rightOffset=getMeasuredWidth()-child.getMeasuredWidth()-getPaddingRight();
                int result=Math.min(Math.max(leftOffset,left),rightOffset);

                return result;
            }

            @Override
            public int clampViewPositionVertical(View child, int top, int dy) {
                int topOffset=getPaddingTop();
                int bottomOffset=getMeasuredHeight()-child.getMeasuredHeight()-topOffset;

                int result=Math.min(Math.max(top,topOffset),bottomOffset);
                return result;
            }

            @Override
            public void onViewDragStateChanged(int state) {
                super.onViewDragStateChanged(state);
            }

            @Override
            public void onViewReleased(View releasedChild, float xvel, float yvel) {
                super.onViewReleased(releasedChild, xvel, yvel);
                //释放后，退出长按模式
                bLongClickMode=false;
                releasedChild.setAlpha(1f);

            }
        });
    }

    @Override
    protected void onLayout(boolean b, int i, int i1, int i2, int i3) {
        float leftOffset=0;
        float topOffset=0;
        float lineHeight=0;
        for(int j=0;j<getChildCount();j++){
            View child=getChildAt(j);
            leftOffset+=mHorizontalSpacing;

            //判断是不是每一行的第一个view，如果是，则左偏移量重置，上偏移量增加一个view与垂直距离的和的高度，以另起一行
            if (j%mColumeSize==0){
                leftOffset=mHorizontalSpacing;
                topOffset+=lineHeight+mVerticalSpacing;
            }
            //保存这一行的最大高度
            lineHeight=child.getMeasuredHeight()>lineHeight?child.getMeasuredHeight():lineHeight;

            child.layout((int)leftOffset,(int)topOffset,(int)(leftOffset+child.getMeasuredWidth()),(int)(topOffset+child.getMeasuredHeight()));
            //左偏移量增加一个view的宽度
            leftOffset+=child.getMeasuredWidth();
        }
    }



    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode=MeasureSpec.getMode(widthMeasureSpec);
        int widthSize=MeasureSpec.getSize(widthMeasureSpec);
        int heightMode=MeasureSpec.getMode(heightMeasureSpec);
        int heightSize=MeasureSpec.getSize(heightMeasureSpec);

        int width=widthSize,height=heightSize;
        MarginLayoutParams lp= (MarginLayoutParams) getLayoutParams();
        if (widthMode==MeasureSpec.AT_MOST){
            width=getScreenWidth()-lp.leftMargin-lp.rightMargin;
        }

        //根据设置的列数计算出行数，有余数则行数+1;
        int row=getChildCount()/mColumeSize;
        if (getChildCount()%mColumeSize!=0){
            row++;
        }
        for(int i=0;i<getChildCount();i++){
            View c=getChildAt(i);
            //
            if (mStretchMode==STRETCH_MODE_NONE){
                measureChild(c,widthMeasureSpec,heightMeasureSpec);
            }else {
                int childWidth= (int) ((width-getPaddingLeft()-getPaddingRight()-mHorizontalSpacing*(mColumeSize+1))/mColumeSize);
                measureChild(c,MeasureSpec.makeMeasureSpec(childWidth,MeasureSpec.EXACTLY),heightMeasureSpec);
            }
        }
        if (heightMode==MeasureSpec.AT_MOST){
            View child=getChildAt(0);
            //计算出子view需要的高度
            height= (int) (row*(child.getMeasuredHeight()+mVerticalSpacing)+mVerticalSpacing);
        }

        setMeasuredDimension(width,height);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(getContext(),null);
    }

    private int getScreenWidth(){
        WindowManager wm= (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay().getWidth();
    }

    @Override
    public boolean onInterceptHoverEvent(MotionEvent event) {
       return mDragHelper.shouldInterceptTouchEvent(event);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                bCancel=false;
                DragRunnable runnable=new DragRunnable(event);
                postDelayed(runnable,LONG_CLICK_MODE_TIME);
                break;
            case MotionEvent.ACTION_MOVE:

                break;
            case MotionEvent.ACTION_UP:
                bCancel=true;
                break;
            case MotionEvent.ACTION_CANCEL:
                bCancel=true;
                break;
        }
        if (bLongClickMode)
            mDragHelper.processTouchEvent(event);
        return true;
    }

    private boolean bLongClickMode=false;
    private boolean bCancel=false;

    /**
     * 进入长按模式
     */
   private class DragRunnable implements Runnable{

       private MotionEvent event;
       public DragRunnable(MotionEvent event){
           this.event=event;
       }
       @Override
       public void run() {
           if(!bCancel){
               mDragHelper.processTouchEvent(event);
               bLongClickMode=true;
               vibrate();
           }

       }
   }


    private void vibrate(){
        Vibrator vibrator= (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }
}
