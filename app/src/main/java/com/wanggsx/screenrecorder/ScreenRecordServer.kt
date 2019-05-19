package com.wanggsx.screenrecorder

import android.R
import android.annotation.TargetApi
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.support.annotation.RequiresApi


import com.hani.coolcode.R
import com.hani.coolcode.utils.CommonUtil
import com.hani.coolcode.utils.FileUtil

import java.io.File
import java.io.IOException


/**
 * Created by admin on 2018/3/28.
 */

class ScreenRecordService : Service(), Handler.Callback {

    private var mProjectionManager: MediaProjectionManager? = null
    private var mMediaProjection: MediaProjection? = null
    private var mMediaRecorder: MediaRecorder? = null
    private var mVirtualDisplay: VirtualDisplay? = null

    private var mIsRunning: Boolean = false
    private val mRecordWidth = UtilsCommon.getScreenWidth()
    private val mRecordHeight = UtilsCommon.getScreenHeight()
    private val mScreenDpi = UtilsCommon.getScreenDpi()


    private var mResultCode: Int = 0
    private var mResultData: Intent? = null

    //录屏文件的保存地址
    var recordFilePath: String? = null
        private set

    private var mHandler: Handler? = null
    //已经录制多少秒了
    private var mRecordSeconds = 0


    val isReady: Boolean
        get() = mMediaProjection != null && mResultData != null

    val saveDirectory: String?
        get() = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {

            Environment.getExternalStorageDirectory().absolutePath
        } else {
            null
        }

    override fun onBind(intent: Intent): IBinder? {
        return RecordBinder()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return Service.START_STICKY
    }

    override fun onCreate() {
        super.onCreate()


        mIsRunning = false
        mMediaRecorder = MediaRecorder()
        mHandler = Handler(Looper.getMainLooper(), this)

    }

    override fun onDestroy() {
        super.onDestroy()
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun clearRecordElement() {
        clearAll()
        if (mMediaRecorder != null) {
            mMediaRecorder!!.reset()
            mMediaRecorder!!.release()
            mMediaRecorder = null
        }
        mResultData = null
        mIsRunning = false
    }

    fun ismIsRunning(): Boolean {
        return mIsRunning
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun setResultData(resultCode: Int, resultData: Intent) {
        mResultCode = resultCode
        mResultData = resultData

        mProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        if (mProjectionManager != null) {
            mMediaProjection = mProjectionManager!!.getMediaProjection(mResultCode, mResultData!!)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun startRecord(): Boolean {
        if (mIsRunning) {
            return false
        }
        if (mMediaProjection == null) {
            mMediaProjection = mProjectionManager!!.getMediaProjection(mResultCode, mResultData!!)

        }

        setUpMediaRecorder()
        createVirtualDisplay()
        mMediaRecorder!!.start()

        ScreenUtil.startRecord()
        //最多录制三分钟
        mHandler!!.sendEmptyMessageDelayed(MSG_TYPE_COUNT_DOWN, 1000)

        mIsRunning = true

        //        Log.w("lala","startRecord ");
        return true
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun stopRecord(tip: String?): Boolean {
        //        Log.w("lala","stopRecord: first ");

        if (!mIsRunning) {
            return false
        }
        mIsRunning = false
        //        Log.w("lala","stopRecord  middle");

        try {
            mMediaRecorder!!.stop()
            mMediaRecorder!!.reset()
            mMediaRecorder = null
            mVirtualDisplay!!.release()
            mMediaProjection!!.stop()

            //            Log.w("lala","stopRecord ");

        } catch (e: Exception) {
            e.printStackTrace()
            mMediaRecorder!!.release()
            mMediaRecorder = null
            //            Log.w("lala","stopRecord  exception");

        }


        mMediaProjection = null

        mHandler!!.removeMessages(MSG_TYPE_COUNT_DOWN)
        ScreenUtil.stopRecord(tip)

        if (mRecordSeconds <= 2) {

            FileUtil.deleteSDFile(recordFilePath)
        } else {
            //通知系统图库更新
            FileUtil.fileScanVideo(this, recordFilePath, mRecordWidth, mRecordHeight, mRecordSeconds)
        }

        //        mRecordFilePath = null;
        mRecordSeconds = 0

        return true
    }


    fun pauseRecord() {
        if (mMediaRecorder != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mMediaRecorder!!.pause()
            }
        }

    }

    fun resumeRecord() {
        if (mMediaRecorder != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mMediaRecorder!!.resume()
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun createVirtualDisplay() {
        mVirtualDisplay = mMediaProjection!!.createVirtualDisplay(
            "MainScreen", mRecordWidth, mRecordHeight, mScreenDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder!!.surface, null, null
        )
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setUpMediaRecorder() {

        recordFilePath = saveDirectory + File.separator + System.currentTimeMillis() + ".mp4"
        if (mMediaRecorder == null) {
            mMediaRecorder = MediaRecorder()
        }
        mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mMediaRecorder!!.setOutputFile(recordFilePath)
        mMediaRecorder!!.setVideoSize(mRecordWidth, mRecordHeight)
        mMediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mMediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        mMediaRecorder!!.setVideoEncodingBitRate((mRecordWidth.toDouble() * mRecordHeight.toDouble() * 3.6).toInt())
        mMediaRecorder!!.setVideoFrameRate(20)

        try {
            mMediaRecorder!!.prepare()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun clearAll() {
        if (mMediaProjection != null) {
            mMediaProjection!!.stop()
            mMediaProjection = null
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {

            MSG_TYPE_COUNT_DOWN -> {

                var str: String? = null
                val enough = FileUtil.getSDFreeMemory() / (1024 * 1024) < 4
                if (enough) {
                    //空间不足，停止录屏
                    str = getString(R.string.record_space_tip)
                    stopRecord(str)
                    mRecordSeconds = 0
                    break
                }

                mRecordSeconds++
                var minute = 0
                var second = 0
                if (mRecordSeconds >= 60) {
                    minute = mRecordSeconds / 60
                    second = mRecordSeconds % 60
                } else {
                    second = mRecordSeconds
                }
                ScreenUtil.onRecording("0" + minute + ":" + if (second < 10) "0$second" else second.toString() + "")

                if (mRecordSeconds < 3 * 60) {
                    mHandler!!.sendEmptyMessageDelayed(MSG_TYPE_COUNT_DOWN, 1000)
                } else if (mRecordSeconds == 3 * 60) {
                    str = getString(R.string.record_time_end_tip)
                    stopRecord(str)
                    mRecordSeconds = 0
                }
            }
        }
        return true
    }

    inner class RecordBinder : Binder() {
        val recordService: ScreenRecordService
            get() = this@ScreenRecordService
    }

    companion object {

        private val MSG_TYPE_COUNT_DOWN = 110
    }


}