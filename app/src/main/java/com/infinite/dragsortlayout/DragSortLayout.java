package com.infinite.dragsortlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Scroller;

/**
 * Created by inf on 2016/12/8.
 */

public class DragSortLayout extends ViewGroup {

    /**
     * 顶部和底部分别距离屏幕顶部和底部的距离
     */
    private int mTopBoarder,mBottomBoarder;
    /**
     * 拖动到目标view时，view的默认缩放比例
     */
    private static final float SCALE_RATION = 0.8f;
    private static final float LONG_CLICK_TOUCH_SLOPE = 7;
    /**
     * 进入长按模式的时间
     */
    private static final int LONG_CLICK_MODE_TIME = 300;
    /**
     * 默认列数
     */
    private static final int DEFAULT_COLUME_SIZE = 3;
    /**
     * 默认垂直间距
     */
    private static final float DEFAULT_VERTICAL_SPACING = 20;
    /**
     * 默认水平间距
     */
    private static final float DEFAULT_HORIZONTAL_SPACING = 20;
    private static final int STRETCH_MODE_COLUME_WIDTH = 0;
    private static final int STRETCH_MODE_NONE = 1;
    private int mColumeSize = DEFAULT_COLUME_SIZE;
    private float mVerticalSpacing = DEFAULT_VERTICAL_SPACING;
    private float mHorizontalSpacing = DEFAULT_HORIZONTAL_SPACING;
    private int mStretchMode = STRETCH_MODE_NONE;


    public DragSortLayout(Context context) {
        this(context, null);
    }

    public DragSortLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragSortLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.DragSortLayout, 0, defStyleAttr);
        mColumeSize = ta.getInteger(R.styleable.DragSortLayout_columeSize, DEFAULT_COLUME_SIZE);
        mHorizontalSpacing = ta.getDimension(R.styleable.DragSortLayout_horizontalSpacing, DEFAULT_HORIZONTAL_SPACING);
        mVerticalSpacing = ta.getDimension(R.styleable.DragSortLayout_horizontalSpacing, DEFAULT_VERTICAL_SPACING);
        mStretchMode = ta.getInt(R.styleable.DragSortLayout_stretchMode, STRETCH_MODE_NONE);
        ta.recycle();
        mScroller=new Scroller(getContext());

    }

    @Override
    protected void onLayout(boolean b, int i, int i1, int i2, int i3) {
        float leftOffset = 0;
        float topOffset = 0;
        float lineHeight = 0;
        for (int j = 0; j < getChildCount(); j++) {
            View child = getChildAt(j);
            leftOffset += mHorizontalSpacing;

            //判断是不是每一行的第一个view，如果是，则左偏移量重置，上偏移量增加一个view与垂直距离的和的高度，以另起一行
            if (j % mColumeSize == 0) {
                leftOffset = mHorizontalSpacing;
                topOffset += lineHeight + mVerticalSpacing;
            }
            //保存这一行的最大高度
            lineHeight = child.getMeasuredHeight() > lineHeight ? child.getMeasuredHeight() : lineHeight;

            child.layout((int) leftOffset,
                         (int) topOffset,
                         (int) (leftOffset + child.getMeasuredWidth()),
                         (int) (topOffset + child.getMeasuredHeight()));
            //左偏移量增加一个view的宽度
            leftOffset += child.getMeasuredWidth();
        }
        Log.e("onLayout","layout");
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width = widthSize, height = heightSize;
        MarginLayoutParams lp = (MarginLayoutParams) getLayoutParams();
        if (widthMode == MeasureSpec.AT_MOST) {
            width = getScreenWidth() - lp.leftMargin - lp.rightMargin;
        }

        //根据设置的列数计算出行数，有余数则行数+1;
        int row = getChildCount() / mColumeSize;
        if (getChildCount() % mColumeSize != 0) {
            row++;
        }
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            //
            if (mStretchMode == STRETCH_MODE_NONE) {
                measureChild(c, widthMeasureSpec, heightMeasureSpec);
            } else {
                int childWidth = (int) ((width - getPaddingLeft() - getPaddingRight() - mHorizontalSpacing * (mColumeSize + 1)) / mColumeSize);
                measureChild(c, MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY), heightMeasureSpec);
            }
        }
        if (heightMode == MeasureSpec.AT_MOST) {
            View child = getChildAt(0);
            //计算出子view需要的高度
            height = (int) (row * (child.getMeasuredHeight() + mVerticalSpacing) + mVerticalSpacing);
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(getContext(), null);
    }

    private int getScreenWidth() {
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay().getWidth();
    }


    private float mLastX, mLastY, mCurrentX, mCurrentY;
    private LongClickRunnable mLongClickRunnable;
    /**
     * 拖动的view
     */
    private View mDragView;
    /**
     * 目标view
     */
    private View mTargetView;

    private float tempY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {


        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastX = event.getX();
                mLastY = event.getY();
                mCurrentX=mLastX;
                mCurrentY=mLastY;
                tempY=mLastY;
                mLongClickRunnable = new LongClickRunnable(mLastX, mLastY);
                postDelayed(mLongClickRunnable, LONG_CLICK_MODE_TIME);
                mDragView = findChildByPoints(mLastX, mLastY);
                break;
            case MotionEvent.ACTION_MOVE:
                mCurrentY = event.getY();
                mCurrentX = event.getX();
                //如果不在长按模式，并且滑动距离超过默认设置的大小，移除长按runnable
                if (!bLongClickMode && mLongClickRunnable != null && !checkForLongClick(mCurrentX - mLastX,
                                                                                        mCurrentY - mLastY)) {
                    removeCallbacks(mLongClickRunnable);
//                    mScroller.startScroll(0, getScrollY(), 0, (int) (mCurrentY-tempY));
                    int dy= (int) (mCurrentY-tempY);

                    int scrollY=getScrollY();

                    //可滑动的最大值
                    int maxScroll=getHeight()-((ViewGroup) getParent()).getHeight();
                    //如果滑动距离超过最大滑动距离，重新设置dy，防止滑出边界
                    if (scrollY-dy>maxScroll){
                        dy=maxScroll-scrollY;
                    }
                    if (scrollY-dy<0){
                        dy=0;
                    }
                    if (scrollY>=0&&scrollY<=maxScroll){
                        scrollBy(0,-dy);
                        tempY=mCurrentY;
                    }

                } else {
                    onActionMove(mCurrentX - mLastX, mCurrentY - mLastY);
                }

                break;
            case MotionEvent.ACTION_UP:
                //如果不在长按模式，并且滑动距离在指定范围内，则认为是点击事件
                if (!bLongClickMode && checkForLongClick(mCurrentX - mLastX, mCurrentY - mLastY)) {
                    processClick(mLastX, mLastY);
                }
                if (mLongClickRunnable != null)
                    removeCallbacks(mLongClickRunnable);
                if (bLongClickMode)
                onLongClickFinish(mDragView, mCurrentX, mCurrentY);

                break;
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return true;
    }

    private boolean bLongClickMode = false;


    /**
     * 长按
     */
    private class LongClickRunnable implements Runnable {

        private float mPointX, mPointY;

        public LongClickRunnable(float pointX, float PointY) {
            this.mPointX = pointX;
            this.mPointY = PointY;
        }

        @Override
        public void run() {
            View view = findChildByPoints(mPointX, mPointY);
            if (view != null) {
                view.setAlpha(0.7f);
                vibrate();
                bLongClickMode = true;
                //获取时时的view边界
                mLeft = view.getLeft();
                mTop = view.getTop();
            }
        }
    }

    /**
     * 通过坐标找到对应的子view
     *
     * @param pointX
     * @param pointY
     * @return
     */
    private View findChildByPoints(float pointX, float pointY) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);

            int left = child.getLeft();
            int top = child.getTop();
            int right = child.getRight();
            int bottom = child.getBottom();

            if (left < pointX && pointX < right && top < pointY && pointY < bottom) {
                return child;
            }
        }
        return null;
    }

    /**
     * 长按事件结束
     *
     * @param view
     * @param pointX
     * @param pointY
     */
    private void onLongClickFinish(View view, float pointX, float pointY) {
        bLongClickMode = false;
        if (view == null)
            return;
        view.setAlpha(1f);
        int dragRight = mLeft + view.getMeasuredWidth();
        int dragBottom = mTop + view.getMeasuredHeight();
        //没有目标view，拖动view放回原处
        if (mTargetView == null) {
            mDragView.layout(mLeft, mTop, dragRight, dragBottom);
            return;
        }
        int targetLeft = mTargetView.getLeft();
        int targetRight = mTargetView.getRight();
        int targetTop = mTargetView.getTop();
        int targetBottom = mTargetView.getBottom();

        int targetPosition=-1,dragPosition=-1;

        for(int i=0;i<getChildCount();i++){
            View child=getChildAt(i);
            if (child==mDragView){
                dragPosition=i;
            }
            if (child==mTargetView){
                targetPosition=i;
            }
        }
        //拖动view和目标view互换位置
//        mDragView.layout(targetLeft, targetTop, targetRight, targetBottom);
//        mTargetView.layout(mLeft, mTop, dragRight, dragBottom);
        //把目标view恢复原来的大小
        scaleView(mTargetView,1);

        //移除拖动view
        removeViewAt(dragPosition);
        //把拖动view add到目标位置
        addView(mDragView,targetPosition);
        //重新布局
        requestLayout();
//        removeViewAt(targetPosition);
//        if (dragPosition>targetPosition){
//            removeViewAt(dragPosition-1);
//        }
//        addView(mTargetView,dragPosition-1);
//        addView(mDragView,dragPosition);


        if (mPositionChangedListener!=null){
            mPositionChangedListener.onPositionChanged(mDragView,mTargetView,dragPosition,targetPosition);
        }
    }

    /**
     * 进入长安模式时的左上坐标
     */
    private int mLeft, mTop;

    private void onActionMove(float dx, float dy) {
        if (mDragView != null && bLongClickMode) {

            //边界控制，不滑出屏幕
            int newLeft = (int) (mLeft + dx);
            int newTop = (int) (mTop + dy);
            if (newLeft <= 0)
                newLeft = 0;
            if (newTop <= 0)
                newTop = 0;

            int newRight = newLeft + mDragView.getMeasuredWidth();
            int newBottom = newTop + mDragView.getMeasuredHeight();
            if (newRight >= getMeasuredWidth())
                newRight = getMeasuredWidth();
//            if (newBottom >= getMeasuredHeight())
//                newBottom = getMeasuredHeight();

            if (newRight == getMeasuredWidth()) {
                newLeft = newRight - mDragView.getMeasuredWidth();
            }
            if (newBottom == getMeasuredHeight()) {
                newTop = newBottom - mDragView.getMeasuredHeight();
            }
            mDragView.layout(newLeft, newTop, newRight, newBottom);
            mTargetView = calculateMaxCoincidePartView(newLeft, newTop, newRight, newBottom);

//            Log.e("gg",newBottom+" "+getHeight());
//            if (newBottom==getHeight()-getPaddingTop()-getPaddingBottom()){
//                mScroller.startScroll(getScrollX(),getScrollY(),0,mDragView.getMeasuredHeight());
//            }
        }
    }

    /**
     * 计算拖动的view与其他view的重合部分面积，以此判定放下的位置
     *
     * @param l 左边距
     * @param t 上边距
     * @param r 右边距
     * @param b 下边距
     */
    private View calculateMaxCoincidePartView(int l, int t, int r, int b) {
        // 找出拖动view四个角所在的view
        // 左上角所在的view
        View ltView = findChildByPoints(l, t);
        // 右上角所在的view
        View rtView = findChildByPoints(r, t);
        // 左下角所在的view
        View lbView = findChildByPoints(l, b);
        // 右下角所在的view
        View rbView = findChildByPoints(r, b);

        //各个角所在view与拖动view重合的面积
        float lt = 0, rt = 0, rb = 0, lb = 0, dragViewProportion;
        if (ltView != null)
            lt = calculateCoincideProportion(l, t, ltView.getRight(), ltView.getBottom());
        if (rtView != null)
            rt = calculateCoincideProportion(r, t, rtView.getLeft(), rtView.getBottom());
        if (rbView != null)
            rb = calculateCoincideProportion(r, b, rbView.getLeft(), rbView.getTop());
        if (lbView != null)
            lb = calculateCoincideProportion(l, b, lbView.getRight(), lbView.getTop());

        dragViewProportion = calculateCoincideProportion(l, t, r, b);

        for (int i = 0; i < getChildCount(); i++) {
            scaleView(getChildAt(i), 1);
        }


        //重合面积大于其他view的重合面积，且大于拖动view面积一半的
        if (lt >= rt && lt >= rb && lt >= lb && lt >= dragViewProportion * 0.5) {
            scaleView(ltView, SCALE_RATION);
            return ltView;
        }
        if (rt >= lt && rt >= rb && rt >= lb && rt >= dragViewProportion * 0.5) {
            scaleView(rtView, SCALE_RATION);
            return rtView;
        }
        if (rb >= rt && rb >= lt && rb >= lb && rb >= dragViewProportion * 0.5) {
            scaleView(rbView, SCALE_RATION);
            return rbView;
        }
        if (lb >= rt && lb >= rb && lb >= lt && lb >= dragViewProportion * 0.5) {
            scaleView(lbView, SCALE_RATION);
            return lbView;
        }
        return null;
    }

    /**
     * 缩放view
     */
    private void scaleView(View view, float ratio) {
        if (view != null) {
            view.setScaleX(ratio);
            view.setScaleY(ratio);
        }
    }

    /**
     * 计算重合部分面积
     *
     * @return
     */
    private float calculateCoincideProportion(int axisX1, int axisY1, int axisX2, int axisY2) {
        return Math.abs(axisX1 - axisX2) * Math.abs(axisY1 - axisY2);
    }

    /**
     * 判断手指滑动的距离，小于默认值则认为是长按事件
     *
     * @param dx
     * @param dy
     * @return
     */
    private boolean checkForLongClick(float dx, float dy) {
        if (Math.abs(dx) <= LONG_CLICK_TOUCH_SLOPE && Math.abs(dy) <= LONG_CLICK_TOUCH_SLOPE) {
            return true;
        }
        return false;
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }

    /**
     * 点击事件回调接口
     */
    public interface OnItemClickListener {
        void onItemClick(View childView, int position);
    }

    private OnItemClickListener mItemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mItemClickListener = listener;
    }

    private void processClick(float pointX, float pointY) {
        View childView = findChildByPoints(pointX, pointY);
        if (childView != null) {
            for (int i = 0; i < getChildCount(); i++) {
                if (childView == getChildAt(i)) {
                    if (mItemClickListener != null) {
                        mItemClickListener.onItemClick(childView, i);
                    }
                }
            }
        }
    }

    public interface OnPositionChangedListener{
        void onPositionChanged(View dragView,View targetView,int dragPosition,int targetPosition);
    }

    private OnPositionChangedListener mPositionChangedListener;
    public void setOnPositionChangedListener(OnPositionChangedListener listener){
        mPositionChangedListener=listener;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            invalidate();
        }
    }

    private Scroller mScroller;
}
