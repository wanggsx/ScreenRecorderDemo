package com.wanggsx.screenrecorder

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView

import android.support.v4.content.PermissionChecker.PERMISSION_DENIED

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var mTvStart: TextView? = null
    private var mTvEnd: TextView? = null

    private var mTvTime: TextView? = null

    private val REQUEST_CODE = 1

    private var mServiceConnection: ServiceConnection? = null

    private val recordListener = object : ScreenUtil.RecordListener {
        override fun onStartRecord() {
            UtilsToast.show(this@MainActivity, "onStartRecord")
        }

        override fun onPauseRecord() {
            UtilsToast.show(this@MainActivity, "onPauseRecord")
        }

        override fun onResumeRecord() {
            UtilsToast.show(this@MainActivity, "onResumeRecord")
        }

        override fun onStopRecord(stopTip: String) {
            UtilsToast.show(this@MainActivity, stopTip)
        }

        override fun onRecording(timeTip: String) {
            mTvTime!!.text = timeTip
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        UtilsCommon.init(this)
        UtilsPermission.checkPermission(this)
        mTvStart = findViewById(R.id.tv_start)
        mTvStart!!.setOnClickListener(this)

        mTvTime = findViewById(R.id.tv_record_time)

        mTvEnd = findViewById(R.id.tv_end)
        mTvEnd!!.setOnClickListener(this)

        startScreenRecordService()

    }

    /**
     * 开启录制 Service
     */
    private fun startScreenRecordService() {

        mServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                val recordBinder = service as ScreenRecordService.RecordBinder
                val screenRecordService = recordBinder.recordService
                ScreenUtil.setScreenService(screenRecordService)
            }

            override fun onServiceDisconnected(name: ComponentName) {

            }
        }

        val intent = Intent(this, ScreenRecordService::class.java)
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)

        ScreenUtil.addRecordListener(recordListener)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (temp in grantResults) {
            if (temp == PERMISSION_DENIED) {
                val dialog = AlertDialog.Builder(this).setTitle("申请权限").setMessage("这些权限很重要").setNegativeButton(
                    "取消"
                ) { dialog, which -> UtilsToast.show(this@MainActivity, "取消") }.setPositiveButton(
                    "设置"
                ) { dialog, which ->
                    val intent = Intent()
                    intent.action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.data = Uri.parse("package:" + this@MainActivity.packageName)
                    this@MainActivity.startActivity(intent)
                }.create()
                dialog.show()
                break
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            try {
                ScreenUtil.setUpData(resultCode, data!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        } else {
            UtilsToast.show(this, "拒绝录屏")
        }
    }

    override fun onClick(v: View) {

        when (v.id) {
            R.id.tv_start -> {
                ScreenUtil.startScreenRecord(this, REQUEST_CODE)
            }
            R.id.tv_end -> {
                ScreenUtil.stopScreenRecord(this)
            }
        }

    }
}