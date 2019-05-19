package com.wanggsx.screenrecorder

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import java.io.File
import android.util.DisplayMetrics
import android.view.Display
import java.io.IOException


public class UtilsRecorder{

    lateinit var mMediaRecorder : MediaRecorder
    lateinit var mRecordFilePath : String

     var mScreenWidth: Int = 0
     var mScreenHeight: Int = 0
     var mScreenDpi: Int = 0

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public fun setUpMediaRecorder(context : Activity) {

        mRecordFilePath = Environment.getExternalStorageDirectory().path + File.separator + System.currentTimeMillis() + ".mp4"
        if (mMediaRecorder == null) {
            val display = context.getWindowManager().getDefaultDisplay()
            val metrics = DisplayMetrics()
            display.getMetrics(metrics)
            mScreenWidth = metrics.widthPixels
            mScreenHeight = metrics.heightPixels
            mScreenDpi = metrics.densityDpi
            mMediaRecorder = MediaRecorder()
        }
        //设置音频来源
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        //设置视频来源
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        //输出的录屏文件格式
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        //录屏文件路径
        mMediaRecorder.setOutputFile(mRecordFilePath)
        //视频尺寸
        mMediaRecorder.setVideoSize(mScreenWidth, mScreenHeight)
        //音视频编码器
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        //比特率
        mMediaRecorder.setVideoEncodingBitRate((mScreenWidth * mScreenHeight * 3.6) as Int)
        //视频帧率
        mMediaRecorder.setVideoFrameRate(20)

        try {
            mMediaRecorder.prepare()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
}