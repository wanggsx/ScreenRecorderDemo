package com.wanggsx.recorddemo

import android.annotation.TargetApi
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.support.v4.content.PermissionChecker
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    //请求码
    private val REQUEST_CODE = 10010
    //自定义的录屏服务
    private lateinit var mService : MyRecordService
    // ServiceConnection
    var mServiceConnection : ServiceConnection = object : ServiceConnection{
        override fun onServiceDisconnected(name: ComponentName?) {

        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            //将service转换为MyRecordService
            var binder : MyRecordService.MyBinder = service as MyRecordService.MyBinder
            mService = binder.recordService
            UtilsScreenRecorder.setScreenService(mService)
        }

    }

    private val mListener = object : UtilsScreenRecorder.RecordListener {
        override fun onStartRecord() {
            Toast.makeText(this@MainActivity,"开始录屏",Toast.LENGTH_LONG).show()
        }

        override fun onStopRecord(stopTip: String) {
            Toast.makeText(this@MainActivity,"结束录屏",Toast.LENGTH_LONG).show()
        }

        override fun onRecording(timeTip: String) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        UtilsCommon.init(this@MainActivity)
        Utils.checkRecorderPermissionAndRequest(this@MainActivity,REQUEST_CODE)
        //绑定服务
        val intent = Intent(this, MyRecordService::class.java)
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)
        UtilsScreenRecorder.mRecordListener = mListener
    }


    fun dostart(v : View){
        UtilsScreenRecorder.startScreenRecordIntent(this@MainActivity,REQUEST_CODE)
    }

    fun dostop(v: View){
        UtilsScreenRecorder.stopScreenRecord(this@MainActivity)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (temp in grantResults) {
            if (temp == PermissionChecker.PERMISSION_DENIED) {
                val dialog = AlertDialog.Builder(this).setTitle("申请权限").setMessage("这些权限很重要").setNegativeButton(
                    "取消"
                ) { dialog, which -> Toast.makeText(this@MainActivity, "取消",Toast.LENGTH_LONG).show() }.setPositiveButton(
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "获取到录屏Intent",Toast.LENGTH_LONG).show()
            try {
                UtilsScreenRecorder.onActivityResultAndStartRecord(resultCode, data!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        } else {
            Toast.makeText(this, "拒绝录屏",Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mServiceConnection)
    }

}
