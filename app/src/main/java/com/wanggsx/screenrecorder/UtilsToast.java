package com.wanggsx.screenrecorder;


import android.content.Context;
import android.widget.Toast;

/**
 * Created by admin on 2017/5/19.
 */

public class UtilsToast {

    private static Toast mToast;

    public static void show(Context context,String msg){
        if(mToast == null){
            mToast = Toast.makeText(context,msg,Toast.LENGTH_SHORT);
        }
        else {
            mToast.setText(msg);
        }
        mToast.show();

    }

}