package com.wanggsx.recorddemo

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import java.io.File

class Utils{
    companion object {
        fun checkRecorderPermissionAndRequest(activity: AppCompatActivity, requestCode: Int): Boolean {
            if (Build.VERSION.SDK_INT >= 23) {
                val checkPermission = (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                        + ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE)
                        + ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        + ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE))
                if (checkPermission != PackageManager.PERMISSION_GRANTED) {
                    //动态申请
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ),
                        requestCode
                    )
                    return false
                } else {
                    return true
                }
            }
            return true
        }

        /**
         * 获取SD卡的剩余容量，单位是Byte
         *
         * @return
         */
        fun getSDFreeMemory(): Long {
            try {
                if (isSDExists()) {
                    val pathFile = Environment.getExternalStorageDirectory()
                    // Retrieve overall information about the space on a filesystem.
                    // This is a Wrapper for Unix statfs().
                    val statfs = StatFs(pathFile.getPath())
                    // 获取SDCard上每一个block的SIZE
                    val nBlockSize = statfs.blockSize.toLong()
                    // 获取可供程序使用的Block的数量
                    // long nAvailBlock = statfs.getAvailableBlocksLong();
                    val nAvailBlock = statfs.availableBlocks.toLong()
                    // 计算SDCard剩余大小Byte
                    return nAvailBlock * nBlockSize
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

            return 0
        }

        /**
         * SD卡存在并可以使用
         */
        fun isSDExists(): Boolean {
            return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        }

        /**
         * 添加到媒体数据库
         *
         * @param context 上下文
         */
        fun fileScanVideo(
            context: Context, videoPath: String, videoWidth: Int, videoHeight: Int,
            videoTime: Int
        ): Uri? {

            val file = File(videoPath)
            if (file.exists()) {

                var uri: Uri? = null

                val size = file.length()
                val fileName = file.getName()
                val dateTaken = System.currentTimeMillis()

                val values = ContentValues(11)
                values.put(MediaStore.Video.Media.DATA, videoPath) // 路径;
                values.put(MediaStore.Video.Media.TITLE, fileName) // 标题;
                values.put(MediaStore.Video.Media.DURATION, videoTime * 1000) // 时长
                values.put(MediaStore.Video.Media.WIDTH, videoWidth) // 视频宽
                values.put(MediaStore.Video.Media.HEIGHT, videoHeight) // 视频高
                values.put(MediaStore.Video.Media.SIZE, size) // 视频大小;
                values.put(MediaStore.Video.Media.DATE_TAKEN, dateTaken) // 插入时间;
                values.put(MediaStore.Video.Media.DISPLAY_NAME, fileName)// 文件名;
                values.put(MediaStore.Video.Media.DATE_MODIFIED, dateTaken / 1000)// 修改时间;
                values.put(MediaStore.Video.Media.DATE_ADDED, dateTaken / 1000) // 添加时间;
                values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")

                val resolver = context.getContentResolver()

                if (resolver != null) {
                    try {
                        uri = resolver!!.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        uri = null
                    }

                }

                if (uri == null) {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(videoPath),
                        arrayOf("video/*"),
                        object : MediaScannerConnection.OnScanCompletedListener {
                            override fun onScanCompleted(path: String, uri: Uri) {

                            }
                        })
                }

                return uri
            }

            return null
        }
    }

}