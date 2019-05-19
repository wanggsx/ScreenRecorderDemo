package com.wanggsx.screenrecorder

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.support.v4.view.MotionEventCompat
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout


/** 仿 iOS 左边缘右滑关闭页面
 * Created by admin on 2017/8/14.
 */

class SliderCloseView @JvmOverloads constructor(
    //    private float mLastDownX;

    private val mContext: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(mContext, attrs, defStyleAttr) {

    //当前有效的PointerId,默认为第一个按下屏幕的手指
    private var mActivePointerId: Int = 0
    //true,mSliderView 当前正被拖拽
    private var mIsBeingDrag: Boolean = false
    private var mInitDownX: Float = 0.toFloat()
    private var mInitDownY: Float = 0.toFloat()
    private var mTouchSlop: Int = 0

    private var mSliderListener: OnSliderListener? = null

    private var mSliderView: View? = null

    private var mCurTranslationX: Float = 0.toFloat()
    private var mIsAnimating: Boolean = false

    //true,mSliderView显示出来
    private var mIsSliderShowing: Boolean = false

    //true,除非动画关闭mSliderView
    private var mIsToHiddlenPage: Boolean = false

    private var mVelocityTracker: VelocityTracker? = null

    val isSliderViewVisible: Boolean
        get() = mSliderView != null && mIsSliderShowing

    init {
        init()
    }

    private fun init() {

        mTouchSlop = ViewConfiguration.get(mContext).scaledTouchSlop
        setBackgroundColor(Color.TRANSPARENT)
    }

    fun setSliderListener(listener: OnSliderListener) {
        mSliderListener = listener
    }

    fun addViewToLayout(view: View?, screenWidth: Int) {

        if (view != null) {
            //需要设置Clickable，子view必须消费掉Down事件，不然
            //后续的 move,up 事件是接收不到的
            view.isClickable = true
            mSliderView = view
            val frParams =
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            this.addView(mSliderView, frParams)

            mCurTranslationX = screenWidth.toFloat()
            //先设置 X 方向的偏移，再开启动画
            //视觉上就可以看到View是从右到左进入页面的
            mSliderView!!.translationX = mCurTranslationX
            actionEnd(false)
        }
    }

    fun clearView() {

        if (mSliderView != null) {
            removeView(mSliderView)
            mSliderView = null

        }
    }

    private fun removeViewFromLayout() {

        if (mSliderView != null) {

            mCurTranslationX = 0f
            mSliderView!!.translationX = mCurTranslationX
            actionEnd(true)

        }

    }

    fun hiddenSliderView() {
        mIsToHiddlenPage = true
        removeViewFromLayout()

    }


    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val action = MotionEventCompat.getActionMasked(ev)
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                //Down 事件触发时，表示有第一个手指接触到屏幕了
                //获取第一个手指Down 的PointerId
                mActivePointerId = ev.getPointerId(0)
                mInitDownX = getMotionEventX(ev)
                //                mLastDownX = mInitDownX;
                mInitDownY = getMotionEventY(ev)
                if (mInitDownX == INVALID_VALUE || mInitDownY == INVALID_VALUE) {
                    mIsBeingDrag = false
                    return super.onInterceptTouchEvent(ev)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val x = getMotionEventX(ev)
                val y = getMotionEventY(ev)

                val diffX = x - mInitDownX
                val diffY = y - mInitDownY

                //手指按下的初始位置在屏幕左侧的 十分之一的范围里，并且 X 方向的距离
                //比 Y 方向上的多，也超过最小的 mTouchSlop，就可以认为已经开始拖拽了
                if (mInitDownX < width / 10 && Math.abs(diffX) >= mTouchSlop
                    && Math.abs(diffX) > Math.abs(diffY)
                ) {
                    mIsBeingDrag = true
                }

            }
            MotionEvent.ACTION_POINTER_UP -> {
                //当有多个手指按在屏幕上，其中一个手指抬起时会进入此方法
                onSecondaryPointerUp(ev)
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                //最后一个手指抬起，或者事件被父view 拦截时，恢复到初始状态
                mIsBeingDrag = false
                mInitDownX = 0f
                mInitDownY = 0f
                //                mLastDownX = 0;
                mActivePointerId = MotionEvent.INVALID_POINTER_ID
            }
        }

        //如果 mIsBeingDrag 为 true ，说明已经触发了滑动的条件
        //事件会被拦截，交给 onTouchEvent 处理
        return mIsBeingDrag || super.onInterceptTouchEvent(ev)
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {

        when (MotionEventCompat.getActionMasked(event)) {

            MotionEvent.ACTION_DOWN -> {
                mInitDownX = getMotionEventX(event)
                mInitDownY = getMotionEventY(event)
            }
            MotionEvent.ACTION_MOVE -> {

                //初始化速度追踪器，用以追踪手指的滑动速度
                if (mVelocityTracker == null) {
                    mVelocityTracker = VelocityTracker.obtain()
                }
                mVelocityTracker!!.addMovement(event)

                val x = getMotionEventX(event)
                val diffX = x - mInitDownX

                if (diffX >= 0) {
                    //手指是向右滑动的，偏移 SliderView
                    if (mSliderView != null) {
                        mSliderView!!.translationX = diffX
                    }
                }

                if (mSliderListener != null) {
                    mSliderListener!!.onProgress(diffX.toInt(), diffX * 1.0f / width, mSliderView)
                }

                Log.w("lala", "getScrollX: " + diffX + " rate: " + diffX * 1.0f / width)

                // 左侧即将滑出屏幕

                return true
            }
            MotionEvent.ACTION_POINTER_UP ->
                //当有多个手指按在屏幕上，其中一个手指抬起时会进入此方法
                onSecondaryPointerUp(event)
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {

                if (mVelocityTracker != null && mActivePointerId != MotionEvent.INVALID_POINTER_ID) {
                    //获取手指抬起的一瞬间，获取 X 方向上的速度
                    mVelocityTracker!!.computeCurrentVelocity(1000)
                    val xVelocity = mVelocityTracker!!.getXVelocity(mActivePointerId)
                    Log.w("tracker", "X velocity: $xVelocity")

                    mVelocityTracker!!.clear()
                    mVelocityTracker = null
                    if (xVelocity >= HORIZANTAL_SPEED && mSliderView != null) {
                        //如果水平的速度超过了特定值，可以认为是手指 fling 操作
                        //让 sliderview 做向右的动画操作，关闭页面
                        mCurTranslationX = mSliderView!!.translationX

                        actionEnd(true)
                        return false
                    }

                }

                // 根据手指释放时的位置决定回弹还是关闭
                val x = getMotionEventX(event)
                val diffX = x - mInitDownX
                if (diffX == 0f) {
                    //手指滑动了 sliderview,但是最后手指抬起时，让它回到了原来的位置
                    if (mSliderListener != null) {
                        mSliderListener!!.onSliderShow(mSliderView)
                    }
                    resetValue()

                } else if (diffX == width.toFloat()) {
                    if (mSliderListener != null) {
                        mSliderListener!!.onSliderHidden()
                    }
                    resetValue()

                } else {

                    if (mSliderView != null) {

                        mCurTranslationX = mSliderView!!.translationX
                        //sliderview 在 水平方向的偏移少于父布局的宽度的一半
                        //则让其回到原位,否则做动画打开
                        if (mCurTranslationX < width / 2) {
                            actionEnd(false)
                        } else {
                            actionEnd(true)
                        }
                    }

                }
            }
        }
        return super.onTouchEvent(event)
    }


    /**
     * 开启动画，
     * @param toRight true,mSliderView滑向右边,否则，滑向左边
     */
    private fun actionEnd(toRight: Boolean) {

        val animator = getAnimator(toRight)
        animator.start()
    }

    private fun getAnimator(toRight: Boolean): ValueAnimator {

        val valueAnimator = ValueAnimator.ofFloat(mCurTranslationX, width as Float)
        valueAnimator.duration = DEFAULT_ANIM_TIME.toLong()
        valueAnimator.interpolator = AccelerateDecelerateInterpolator()
        valueAnimator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            if (mSliderView != null) {
                mSliderView!!.translationX = value

            }

            if (mSliderListener != null) {
                mSliderListener!!.onProgress(value.toInt(), value * 1.0f / width as Float, mSliderView)
            }
        }

        valueAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                mIsAnimating = true

            }

            override fun onAnimationEnd(animation: Animator) {


                if (toRight) {

                    if (mSliderListener != null) {
                        mSliderListener!!.onSliderHidden()
                    }
                    mIsSliderShowing = false
                    mCurTranslationX = width.toFloat()

                    clearView()
                    if (mIsToHiddlenPage) {
                        mIsToHiddlenPage = false
                    }

                } else {

                    if (mSliderListener != null) {
                        mSliderListener!!.onSliderShow(mSliderView)
                    }
                    mIsSliderShowing = true
                    mCurTranslationX = 0f
                }

                resetValue()


            }

            override fun onAnimationCancel(animation: Animator) {

            }

            override fun onAnimationRepeat(animation: Animator) {

            }
        })

        return valueAnimator


    }


    /**
     * 清楚一些记录的变量
     */
    private fun resetValue() {

        mInitDownX = 0f
        mInitDownY = 0f

        mIsBeingDrag = false
        mIsAnimating = false
        mActivePointerId = MotionEvent.INVALID_POINTER_ID

    }

    /**
     * 当屏幕上有手指抬起时，判断是不是 Down 事件触发时记录的 PointerId
     * 如果是的话，选其他手指的 PointerId 作为 mActivePointerId
     * @param event
     */
    private fun onSecondaryPointerUp(event: MotionEvent) {
        val pointerIndex = MotionEventCompat.getActionIndex(event)
        val pointerId = event.getPointerId(pointerIndex)
        if (pointerId == mActivePointerId) {
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            mActivePointerId = event.getPointerId(newPointerIndex)
        }

    }


    /**
     * 获取当前有效PointerId 的 X 值
     * @param event
     * @return
     */
    private fun getMotionEventX(event: MotionEvent): Float {
        val pointerIndex = event.findPointerIndex(mActivePointerId)
        return if (pointerIndex < 0) INVALID_VALUE else event.getX(pointerIndex)
    }

    /**
     * 获取当前有效PointerId 的 Y 值
     * @param event
     * @return
     */
    private fun getMotionEventY(event: MotionEvent): Float {
        val pointerIndex = event.findPointerIndex(mActivePointerId)
        return if (pointerIndex < 0) INVALID_VALUE else event.getY(pointerIndex)
    }


    interface OnSliderListener {
        //判断打开的进度
        fun onProgress(current: Int, progress: Float, view: View?)

        //页面关闭
        fun onSliderHidden()

        //页面打开
        fun onSliderShow(page: View?)
    }

    companion object {

        private val INVALID_VALUE = -1f
        private val DEFAULT_ANIM_TIME = 300
        private val HORIZANTAL_SPEED = 2500f
    }
}