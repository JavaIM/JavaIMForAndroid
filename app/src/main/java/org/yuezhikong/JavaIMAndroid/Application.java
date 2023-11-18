package org.yuezhikong.JavaIMAndroid;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.multidex.MultiDexApplication;

import java.util.List;

public class Application extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 30) {//当大于等于Android 11（API 30）时读取上一次的退出原因
            ActivityManager manager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
            List<ApplicationExitInfo> exitInfo = manager.getHistoricalProcessExitReasons(this.getPackageName(), 0, 1);
            if (!exitInfo.isEmpty())
            {
                ApplicationExitInfo info = exitInfo.get(0);
                Log.i("JavaIM","The Last Exit Reason:"+info.getReason());
                Log.i("JavaIM","TimeStamp:"+info.getTimestamp());
            }
        }
    }

}
