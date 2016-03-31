package com.kyletung.dotsprogressbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

/**
 * All rights reserved by Author<br>
 * Author: Dong YuHui<br>
 * Email: <a href="mailto:dyh920827@gmail.com">dyh920827@gmail.com</a><br>
 * Blog: <a href="http://www.kyletung.com">www.kyletung.com</a><br>
 * Create Time: 2016/03/28 at 21:15<br>
 * 用于分段显示进度的进度条
 */
public class DotsProgressBar extends View {

    /**
     * 进度条的点数
     */
    private int mDotsCount;

    /**
     * 进度条圆点的半径
     */
    private int mDotsRadius;

    /**
     * 进度条的宽度
     */
    private int mDotsProgressWidth;

    /**
     * 进度条宽度的一半
     */
    private int mDotsProgressWidthHalf;

    /**
     * 进度条的背景色
     */
    private int mDotsBackColor;

    /**
     * 进度条的前景色
     */
    private int mDotsFrontColor;

    /**
     * 进度条前进的速度
     */
    private int mSpeed;

    /**
     * 通过速度算出的时间最小单位
     */
    private int mTimeGap;

    /**
     * 目前已经进行的时间
     */
    private int mPartTime;

    /**
     * 画笔
     */
    private Paint mPaint;

    /**
     * 原先的进度在某个点
     */
    private int mOldPosition = -1;

    /**
     * 新的进度在某个点
     */
    private int mNewPosition = -1;

    /**
     * 控件宽度
     */
    private int mWidth;

    /**
     * 控件高度
     */
    private int mHeight;

    /**
     * 每段矩形的长度
     */
    private int mPartWidth;

    /**
     * 使用插值器来获得进度值，这样不仅简化了获取进度值的过程，还可以向外提供可定制性
     */
    private Interpolator mInterpolator;

    public DotsProgressBar(Context context) {
        this(context, null);
    }

    public DotsProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DotsProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 获取自定义属性
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.DotsProgressBar);
        mDotsCount = typedArray.getInt(R.styleable.DotsProgressBar_barDotsCount, 2);
        mDotsRadius = typedArray.getDimensionPixelSize(R.styleable.DotsProgressBar_barDotsRadius, dp2px(8));
        mDotsProgressWidth = typedArray.getDimensionPixelSize(R.styleable.DotsProgressBar_barProgressWidth, dp2px(8));
        if ((2 * mDotsRadius) < mDotsProgressWidth)
            mDotsProgressWidth = mDotsRadius * 2; // 如果用户设置进度条的宽度大于点的直径，则设置为半径大小
        mDotsProgressWidthHalf = mDotsProgressWidth / 2;
        // 获取用户定义的时间，并转化成时间间隔
        mSpeed = typedArray.getInt(R.styleable.DotsProgressBar_barSpeed, 40);
        mTimeGap = 1;
        // 获取用户定义的进度条背景色与前景色
        mDotsBackColor = typedArray.getColor(R.styleable.DotsProgressBar_barBackColor, ContextCompat.getColor(context, android.R.color.darker_gray));
        mDotsFrontColor = typedArray.getColor(R.styleable.DotsProgressBar_barFrontColor, ContextCompat.getColor(context, android.R.color.holo_blue_light));
        typedArray.recycle();
        // 初始化画笔
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        // 初始化插值器
        mInterpolator = new LinearInterpolator();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, mDotsRadius * 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();
        mPartWidth = (mWidth - mDotsRadius * 2) / (mDotsCount - 1);
        // 画背景
        mPaint.setColor(mDotsBackColor);
        canvas.drawRect(mDotsRadius, mHeight / 2 - mDotsProgressWidthHalf, mWidth - mDotsRadius, mHeight / 2 + mDotsProgressWidthHalf, mPaint);
        for (int i = 0; i < mDotsCount; i++) {
            canvas.drawCircle(mDotsRadius + mPartWidth * i, mDotsRadius, mDotsRadius, mPaint);
        }
        // 调整画笔为前景色
        mPaint.setColor(mDotsFrontColor);
        // 画切换过程中不变的部分
        drawNotChange(canvas, mPaint, Math.min(mOldPosition, mNewPosition));
        // 画变化的部分
        int start = mOldPosition * mPartWidth + mDotsRadius;
        if (mOldPosition < 0) start = mDotsRadius;
        if (mNewPosition > mOldPosition) {
            // 进度条向前
            mPartTime = mPartTime + mTimeGap;
            int[] params = getForwardParams();
            canvas.drawRect(start, mHeight / 2 - mDotsProgressWidthHalf, start + params[0], mHeight / 2 + mDotsProgressWidthHalf, mPaint);
            canvas.drawCircle(start + params[0], mHeight / 2, params[1], mPaint);
            if (mPartTime < (mTimeGap * mSpeed)) {
                postInvalidateDelayed(1);
            } else {
                mOldPosition = mNewPosition;
            }
        }
        if (mNewPosition < mOldPosition) {
            // 进度条向后
            mPartTime = mPartTime + mTimeGap;
            int[] params = getBackParams();
            canvas.drawRect(start, mHeight / 2 - mDotsProgressWidthHalf, start + params[0], mHeight / 2 + mDotsProgressWidthHalf, mPaint);
            canvas.drawCircle(start + params[0], mHeight / 2, params[1], mPaint);
            if (mPartTime < (mTimeGap * mSpeed)) {
                postInvalidateDelayed(1);
            } else {
                mOldPosition = mNewPosition;
            }
        }
    }

    /**
     * 绘制在变化过程中仍然不变的前景部分
     *
     * @param canvas   画布
     * @param paint    画笔
     * @param position 不变的位置
     */
    private void drawNotChange(Canvas canvas, Paint paint, int position) {
        if (position >= 0) {
            // 画矩形
            if (position != 0) {
                canvas.drawRect(mDotsRadius, mHeight / 2 - mDotsProgressWidthHalf, mDotsRadius + mPartWidth * position, mHeight / 2 + mDotsProgressWidthHalf, mPaint);
            }
            // 画圆
            for (int i = 0; i < (position + 1); i++) {
                canvas.drawCircle(mDotsRadius + mPartWidth * i, mDotsRadius, mDotsRadius, paint);
            }
        }
    }

    /**
     * 进度条向前进一
     */
    public void startForward() {
        if (mOldPosition < mDotsCount) {
            mNewPosition = mOldPosition + 1;
            mPartTime = 0;
            invalidate();
        }
    }

    public void startBack() {
        if (mOldPosition >= 0) {
            mNewPosition = mOldPosition - 1;
            mPartTime = 0;
            invalidate();
        }
    }

    /**
     * 设置插值器
     *
     * @param interpolator 插值器
     */
    public void setInterpolator(Interpolator interpolator) {
        mInterpolator = interpolator;
    }

    /**
     * 通过该方法返回需要绘制的进度条的长度及圆点的半径，向前
     *
     * @return 返回一个整型数组，数组第一个表示进度条的长度，数组第二个表示圆点的半径
     */
    private int[] getForwardParams() {
        final int[] params = new int[2];
        float x = (float) mPartTime / (mSpeed * mTimeGap);
        float y = mInterpolator.getInterpolation(x);
        if (mOldPosition < 0) {
            params[0] = 0;
            params[1] = (int) (y * mDotsRadius);
        } else if (mOldPosition == mDotsCount - 1) {
            params[0] = 0;
            params[0] = 0;
        } else {
            if (y < 0.9) {
                // 此时进度条还没有进入节点
                params[0] = (int) (mPartWidth * (y / 0.9));
                params[1] = mDotsProgressWidthHalf;
            } else {
                // 此时进度条的长条已经加载完毕，还有终点的变化
                params[0] = mPartWidth;
                params[1] = (int) ((mDotsRadius - mDotsProgressWidthHalf) * ((y - 0.9) / 0.1)) + mDotsProgressWidthHalf;
            }
        }
        return params;
    }

    /**
     * 通过该方法返回需要绘制的进度条的长度及圆点的半径，向后
     *
     * @return 返回一个整型数组，数组第一个表示进度条的长度，数组第二个表示圆点的半径
     */
    private int[] getBackParams() {
        final int[] params = new int[2];
        float x = 1 - (float) mPartTime / (mSpeed * mTimeGap);
        float y = mInterpolator.getInterpolation(x);
        if (mNewPosition < 0) {
            params[0] = 0;
            params[1] = (int) (y * mDotsRadius);
        } else if (mOldPosition < 0) {
            params[0] = 0;
            params[0] = 0;
        } else {
            if (y < 0.9) {
                // 此时进度条还没有进入节点
                params[0] = (int) (mPartWidth * (y / 0.9));
                params[1] = mDotsProgressWidthHalf;
            } else {
                // 此时进度条的长条已经加载完毕，还有终点的变化
                params[0] = mPartWidth;
                params[1] = (int) ((mDotsRadius - mDotsProgressWidthHalf) * ((y - 0.9) / 0.1)) + mDotsProgressWidthHalf;
            }
        }
        return params;
    }

    /**
     * dp 转 px
     *
     * @param dpValue dp 值
     * @return 返回 px 值
     */
    private int dp2px(int dpValue) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

}
