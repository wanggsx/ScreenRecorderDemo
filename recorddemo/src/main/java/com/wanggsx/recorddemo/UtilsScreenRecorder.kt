package com.wanggsx.recorddemo

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.support.annotation.RequiresApi
import android.widget.Toast

/**
 * Created by admin on 2018/3/28.
 */

object UtilsScreenRecorder {


    private var mService: MyRecordService? = null

    lateinit var mRecordListener : RecordListener

    /**
     * 获取录制后的文件地址
     * @return
     */
    val screenRecordFilePath: String?
        get() = if (mService != null) {
            mService!!.filePath
        } else null

    /**
     * 判断当前是否在录制
     * @return
     */
    val isRecording: Boolean
        get() {
            return if (mService != null) {
                mService!!.mIsRunning
            } else false
        }


    fun setScreenService(screenService: MyRecordService) {
        mService = screenService
    }

    fun clear() {
        if (mService != null) {
            mService = null

        }
    }

    /**
     * 开始录制
     */
    fun startScreenRecordIntent(activity: Activity, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mService != null && !mService!!.mIsRunning) {
                if (!mService!!.isReady) {
                    //如果没有初始化则先初始化
                    val mediaProjectionManager =
                        activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    if (mediaProjectionManager != null) {
                        //获取录屏intent对象
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
                    //如果初始化完毕，则可以直接开始录屏
                    mService!!.startRecord(0,null)

                }

            }
        }

    }

    /**
     * 获取用户允许录屏后，调用Service中的录屏方法进行录屏
     * @param resultCode
     * @param resultData
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Throws(Exception::class)
    fun onActivityResultAndStartRecord(resultCode: Int, resultData: Intent) {
            if (mService != null && !mService!!.mIsRunning) {
                mService!!.startRecord(resultCode, resultData)

            }
    }

    /**
     * 停止录制
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun stopScreenRecord(context: Context) {
            if (mService != null && mService!!.mIsRunning) {
                mService!!.stopRecord()
            }
    }

    fun onStartRecord() {
        if(mRecordListener!=null) {
            mRecordListener.onStartRecord()
        }
    }

    fun onRecording(timeTip: String) {
        if(mRecordListener!=null) {
            mRecordListener.onRecording(timeTip)
        }
    }

    fun onStopRecord(stopTip: String) {
        if(mRecordListener!=null) {
            mRecordListener.onStopRecord(stopTip)
        }
    }

    fun onStopRecord() {
        if(mRecordListener!=null) {
            mRecordListener.onStopRecord("停止录屏了")
        }
    }

    interface RecordListener {
        fun onStartRecord()
        fun onStopRecord(stopTip: String)
        fun onRecording(timeTip: String)
    }
}