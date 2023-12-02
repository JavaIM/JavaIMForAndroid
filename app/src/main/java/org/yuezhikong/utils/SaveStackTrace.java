package org.yuezhikong.utils;

import android.util.Log;

/**
 * jdk版复制来的代码需要调用此类
 */
public class SaveStackTrace {
    public static void saveStackTrace(Throwable throwable)
    {
        Log.d("JavaIM","出现错误");
        throwable.printStackTrace();
    }
}
