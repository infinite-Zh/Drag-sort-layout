package com.infinite.dragsortlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

/**
 * Created by inf on 2016/12/8.
 */

public class DragSortLayout extends ViewGroup{

    private static final float LONG_CLICK_TOUCH_SLOPE=7;
    /**
     * 进入长按模式的时间
     */
    private static final int LONG_CLICK_MODE_TIME=300;
    /**
     * 默认列数
     */
    private static final int DEFAULT_COLUME_SIZE=3;
    /**
     * 默认垂直间距
     */
    private static final float DEFAULT_VERTICAL_SPACING=20;
    /**
     * 默认水平间距
     */
    private static final float DEFAULT_HORIZONTAL_SPACING=20;
    private static final int STRETCH_MODE_COLUME_WIDTH=0;
    private static final int STRETCH_MODE_NONE=1;
    private int mColumeSize=DEFAULT_COLUME_SIZE;
    private float mVerticalSpacing=DEFAULT_VERTICAL_SPACING;
    private float mHorizontalSpacing=DEFAULT_HORIZONTAL_SPACING;
    private int mStretchMode=STRETCH_MODE_NONE;


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



    private float mLastX,mLastY, mCurrentX,mCurrentY;
    private LongClickRunnable mLongClickRunnable;
    private View mDragView;
    @Override
    public boolean onTouchEvent(MotionEvent event) {


        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                mLastX=event.getX();
                mLastY=event.getY();
                mLongClickRunnable=new LongClickRunnable(mLastX,mLastY);
                postDelayed(mLongClickRunnable,LONG_CLICK_MODE_TIME);
                mDragView=findChildByPoints(mLastX,mLastY);
                break;
            case MotionEvent.ACTION_MOVE:
                mCurrentY=event.getY();
                mCurrentX =event.getX();
                //如果不在长按模式，并且滑动距离超过默认设置的大小，移除长按runnable
                if (!bLongClickMode&&mLongClickRunnable!=null&&!checkForLongClick(mCurrentX -mLastX,mCurrentY-mLastY)){
                    removeCallbacks(mLongClickRunnable);
                }else {
                    onActionMove(mCurrentX -mLastX,mCurrentY-mLastY);
                }

                break;
            case MotionEvent.ACTION_UP:
                //如果不在长按模式，并且滑动距离在指定范围内，则认为是点击事件
                if (!bLongClickMode&&checkForLongClick(mCurrentX -mLastX,mCurrentY-mLastY)){
                    processClick(mLastX,mLastY);
                }
                if (mLongClickRunnable!=null)
                removeCallbacks(mLongClickRunnable);
                onLongClickFinish(mDragView, mCurrentX,mCurrentY);

                break;
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return true;
    }

    private boolean bLongClickMode=false;


    /**
     * 长按
     */
    private class LongClickRunnable implements Runnable{

        private float mPointX,mPointY;
        public LongClickRunnable(float pointX,float PointY){
            this.mPointX=pointX;
            this.mPointY=PointY;
        }
        @Override
        public void run() {
            View view=findChildByPoints(mPointX,mPointY);
            if (view!=null){
                view.setAlpha(0.7f);
                vibrate();
                bLongClickMode=true;
                //获取时时的view边界
                mLeft=view.getLeft();
                mTop=view.getTop();
            }
        }
    }

    /** 通过坐标找到对应的子view
     * @param pointX
     * @param pointY
     * @return
     */
    private View findChildByPoints(float pointX,float pointY){
        for(int i=0;i<getChildCount();i++){
            View child=getChildAt(i);

            int left=child.getLeft();
            int top=child.getTop();
            int right=child.getRight();
            int bottom=child.getBottom();

            if (left<pointX&&pointX<right&&top<pointY&&pointY<bottom){
                return child;
            }
        }
        return null;
    }

    /**
     * 长按事件结束
     * @param view
     * @param pointX
     * @param pointY
     */
    private void onLongClickFinish(View view,float pointX,float pointY){
        bLongClickMode=false;
        if (view!=null){
            view.setAlpha(1f);
        }
    }

    /**
     * 进入长安模式时的左上坐标
     */
    private int mLeft,mTop;

    private void onActionMove(float dx,float dy){
        if (mDragView!=null&&bLongClickMode){

            //边界控制，不滑出屏幕
            int newLeft= (int) (mLeft+dx);
            int newTop= (int) (mTop+dy);
            if (newLeft<=0)
                newLeft=0;
            if (newTop<=0)
                newTop=0;

            int newRight=newLeft+mDragView.getMeasuredWidth();
            int newBottom=newTop+mDragView.getMeasuredHeight();
            if (newRight>=getMeasuredWidth())
                newRight=getMeasuredWidth();
            if (newBottom>=getMeasuredHeight())
                newBottom=getMeasuredHeight();

            if (newRight==getMeasuredWidth()){
                newLeft=newRight-mDragView.getMeasuredWidth();
            }
            if (newBottom==getMeasuredHeight()){
                newTop=newBottom-mDragView.getMeasuredHeight();
            }
            mDragView.layout(newLeft,newTop,newRight,newBottom);
        }
    }

    /**
     * 判断手指滑动的距离，小于默认值则认为是长按事件
     * @param dx
     * @param dy
     * @return
     */
    private boolean checkForLongClick(float dx,float dy){
        if (Math.abs(dx)<=LONG_CLICK_TOUCH_SLOPE&&Math.abs(dy)<=LONG_CLICK_TOUCH_SLOPE){
            return true;
        }
        return false;
    }
    private void vibrate(){
        Vibrator vibrator= (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }

    public interface OnItemClickListener{
        void onItemClick(View childView,int position);
    }

    private OnItemClickListener mItemClickListener;
    public void setOnItemClickListener(OnItemClickListener listener){
        this.mItemClickListener=listener;
    }

    private void processClick(float pointX,float pointY){
        View childView=findChildByPoints(pointX,pointY);
        if (childView!=null){
            for(int i=0;i<getChildCount();i++){
                if (childView==getChildAt(i)){
                    if (mItemClickListener!=null){
                        mItemClickListener.onItemClick(childView,i);
                    }
                }
            }
        }
    }
}
