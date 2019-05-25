package com.wanggsx.recorddemo

import android.annotation.TargetApi
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.support.annotation.RequiresApi
import android.util.Log
import java.io.File
import java.io.IOException
import java.util.*

class MyRecordService : Service(){
    //录屏服务对应的manager，用于创建和查找MediaProjection对象
    private var mProjectionManager : MediaProjectionManager ? = null
    //录屏辅助类，可用于创建录屏所需的虚拟显示屏VirtualDisplay
    private var mMediaProjection : MediaProjection ?= null
    //虚拟显示屏对象
    private lateinit var mVirtualDisplay: VirtualDisplay
    //屏幕宽、高像素
    var mRecordWidth = UtilsCommon.getScreenWidth()
    var mRecordHeight = UtilsCommon.getScreenHeight()
    //由我们的Activity发送mResultData这个Intent
    private var mResultCode: Int = 0
    //录屏Intent，屏幕时需要发送此Intent
    private var mResultData: Intent? = null
    //录屏API类
    private var mMediaRecorder: MediaRecorder? = null
    //录屏文件存储目录
    var filePath: String? = null
    //录屏开始时间
    private lateinit var mStartTime : Date
    //是否正在录屏
    var mIsRunning: Boolean = false
    //是否可以开始录屏了
    val isReady: Boolean
        get() = mMediaProjection != null && mResultData != null

    //Service中必须实现的方法，用于返回一个Binder对象，通过该Binder对象获取到对应的Service对象
    override fun onBind(intent: Intent?): IBinder? {
        return MyBinder()
    }
    //binder对象
    inner class MyBinder : Binder() {
        val recordService: MyRecordService
        get() = this@MyRecordService
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return Service.START_STICKY
    }

    override fun onCreate() {
        //初始化一个MediaRecorder对象
        mIsRunning = false
        mMediaRecorder = MediaRecorder()
        //获取底层服务MEDIA_PROJECTION_SERVICE对应的MediaProjectionManager对象
        mProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun startRecord(resultCode: Int, resultData: Intent?): Boolean {
        if (mIsRunning) {
            return false
        }
        if(resultData!=null){
            mResultCode = resultCode
            mResultData = resultData
        }
        if (mMediaProjection == null) {
            //根据结果码和Intent对象获取对应的MediaProjection对象
            mMediaProjection = mProjectionManager!!.getMediaProjection(mResultCode, mResultData!!)

        }
        setUpMediaRecorder()
        createVirtualDisplay()
        mMediaRecorder!!.start()
        mStartTime = Date()
        mIsRunning = true
        UtilsScreenRecorder.onStartRecord()
        mIsRunning = true
        return true
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun createVirtualDisplay() {
        mVirtualDisplay = mMediaProjection!!.createVirtualDisplay(
            "MainScreen", mRecordWidth, mRecordHeight, UtilsCommon.getScreenDpi(),
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder!!.surface, null, null
        )
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setUpMediaRecorder() {
        filePath = Environment.getExternalStorageDirectory().absolutePath + File.separator + "screenRecording-" + System.currentTimeMillis() + ".mp4"
        if (mMediaRecorder == null) {
            mMediaRecorder = MediaRecorder()
        }
        mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mMediaRecorder!!.setOutputFile(filePath)
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
    fun stopRecord(): Boolean {
        if (!mIsRunning) {
            return false
        }
        mIsRunning = false
        try {
            mMediaRecorder!!.stop()
            mMediaRecorder!!.reset()
            mMediaRecorder = null
            mVirtualDisplay!!.release()
            mMediaProjection!!.stop()
        } catch (e: Exception) {
            e.printStackTrace()
            mMediaRecorder!!.release()
            mMediaRecorder = null
        }
        var dur : Int = ((Date().time - mStartTime.time)/1000).toInt()
        Log.d("wanggsx", "视频持续时间：$dur")
        mMediaProjection = null
        UtilsScreenRecorder.onStopRecord()
        //通知系统图库更新
        Utils.fileScanVideo(this, filePath!!, mRecordWidth, mRecordHeight, dur)
        return true
    }


}