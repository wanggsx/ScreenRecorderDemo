package com.wanggsx.screenrecorder

import android.R
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.widget.Toast
import java.util.ArrayList

/**
 * Created by admin on 2018/3/28.
 */

object ScreenUtil {


    private var s_ScreenRecordService: ScreenRecordService? = null

    private val s_RecordListener = ArrayList<RecordListener>()

    private val s_PageRecordListener = ArrayList<OnPageRecordListener>()

    //true,录制结束的提示语正在显示
    /**
     * true,录制结束的提示语正在显示
     * @return
     */
    var isRecodingTipShow = false

    /**
     * 录屏功能 5.0+ 的手机才能使用
     * @return
     */
    val isScreenRecordEnable: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

    /**
     * 获取录制后的文件地址
     * @return
     */
    val screenRecordFilePath: String?
        get() = if (isScreenRecordEnable && s_ScreenRecordService != null) {
            s_ScreenRecordService!!.recordFilePath
        } else null

    /**
     * 判断当前是否在录制
     * @return
     */
    val isCurrentRecording: Boolean
        get() {
            return if (isScreenRecordEnable && s_ScreenRecordService != null) {
                s_ScreenRecordService!!.ismIsRunning()
            } else false
        }


    fun setScreenService(screenService: ScreenRecordService) {
        s_ScreenRecordService = screenService
    }

    fun clear() {
        if (isScreenRecordEnable && s_ScreenRecordService != null) {
            s_ScreenRecordService!!.clearAll()
            s_ScreenRecordService = null

        }

        if (s_RecordListener != null && s_RecordListener.size > 0) {
            s_RecordListener.clear()
        }

        if (s_PageRecordListener != null && s_PageRecordListener.size > 0) {
            s_PageRecordListener.clear()
        }
    }

    /**
     * 开始录制
     */
    fun startScreenRecord(activity: Activity, requestCode: Int) {

        if (isScreenRecordEnable) {

            if (s_ScreenRecordService != null && !s_ScreenRecordService!!.ismIsRunning()) {

                if (!s_ScreenRecordService!!.isReady) {

                    val mediaProjectionManager =
                        activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    if (mediaProjectionManager != null) {
                        val intent = mediaProjectionManager.createScreenCaptureIntent()
                        val packageManager = activity.packageManager
                        if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                            //存在录屏授权的Activity
                            activity.startActivityForResult(intent, requestCode)
                        } else {
                            Toast.makeText(activity, "can not record", Toast.LENGTH_SHORT).show()
                        }
                    }

                } else {
                    s_ScreenRecordService!!.startRecord()

                }

            }
        }

    }

    /**
     * 获取用户允许录屏后，设置必要的数据
     * @param resultCode
     * @param resultData
     */
    @Throws(Exception::class)
    fun setUpData(resultCode: Int, resultData: Intent) {

        if (isScreenRecordEnable) {

            if (s_ScreenRecordService != null && !s_ScreenRecordService!!.ismIsRunning()) {
                s_ScreenRecordService!!.setResultData(resultCode, resultData)
                s_ScreenRecordService!!.startRecord()

            }

        }
    }

    /**
     * 停止录制
     */
    fun stopScreenRecord(context: Context) {
        if (isScreenRecordEnable) {
            if (s_ScreenRecordService != null && s_ScreenRecordService!!.ismIsRunning()) {
                s_ScreenRecordService!!.stopRecord("tip")
            }
        }
    }

    fun setRecordingStatus(isShow: Boolean) {
        isRecodingTipShow = isShow
    }


    /**
     * 系统正在录屏，app 录屏会有冲突，清理掉一些数据
     */
    fun clearRecordElement() {

        if (isScreenRecordEnable) {
            if (s_ScreenRecordService != null) {
                s_ScreenRecordService!!.clearRecordElement()
            }
        }
    }

    fun addRecordListener(listener: RecordListener?) {

        if (listener != null && !s_RecordListener.contains(listener)) {
            s_RecordListener.add(listener)
        }

    }

    fun removeRecordListener(listener: RecordListener?) {
        if (listener != null && s_RecordListener.contains(listener)) {
            s_RecordListener.remove(listener)
        }
    }

    fun addPageRecordListener(listener: OnPageRecordListener?) {

        if (listener != null && !s_PageRecordListener.contains(listener)) {
            s_PageRecordListener.add(listener)
        }
    }

    fun removePageRecordListener(listener: OnPageRecordListener?) {

        if (listener != null && s_PageRecordListener.contains(listener)) {
            s_PageRecordListener.remove(listener)
        }
    }

    fun onPageRecordStart() {
        if (s_PageRecordListener != null && s_PageRecordListener.size > 0) {
            for (listener in s_PageRecordListener) {
                listener.onStartRecord()
            }
        }
    }


    fun onPageRecordStop() {
        if (s_PageRecordListener != null && s_PageRecordListener.size > 0) {
            for (listener in s_PageRecordListener) {
                listener.onStopRecord()
            }
        }
    }

    fun onPageBeforeShowAnim() {
        if (s_PageRecordListener != null && s_PageRecordListener.size > 0) {
            for (listener in s_PageRecordListener) {
                listener.onBeforeShowAnim()
            }
        }
    }

    fun onPageAfterHideAnim() {
        if (s_PageRecordListener != null && s_PageRecordListener.size > 0) {
            for (listener in s_PageRecordListener) {
                listener.onAfterHideAnim()
            }
        }
    }

    fun startRecord() {
        if (s_RecordListener.size > 0) {
            for (listener in s_RecordListener) {
                listener.onStartRecord()
            }
        }
    }

    fun pauseRecord() {
        if (s_RecordListener.size > 0) {
            for (listener in s_RecordListener) {
                listener.onPauseRecord()
            }
        }
    }

    fun resumeRecord() {
        if (s_RecordListener.size > 0) {
            for (listener in s_RecordListener) {
                listener.onResumeRecord()
            }
        }
    }

    fun onRecording(timeTip: String) {
        if (s_RecordListener.size > 0) {
            for (listener in s_RecordListener) {
                listener.onRecording(timeTip)
            }
        }
    }

    fun stopRecord(stopTip: String) {
        if (s_RecordListener.size > 0) {
            for (listener in s_RecordListener) {
                listener.onStopRecord(stopTip)
            }
        }
    }

    interface RecordListener {


        fun onStartRecord()
        fun onPauseRecord()
        fun onResumeRecord()
        fun onStopRecord(stopTip: String)
        fun onRecording(timeTip: String)
    }


    interface OnPageRecordListener {

        fun onStartRecord()
        fun onStopRecord()

        fun onBeforeShowAnim()
        fun onAfterHideAnim()
    }
}