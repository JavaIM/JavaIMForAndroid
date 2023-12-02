package org.yuezhikong.JavaIMAndroid;

import android.util.Log;

import androidx.annotation.NonNull;

public class NonCrashThreadUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler{
    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
        Log.d("JavaIM Thread",thread.getName()+"出现错误"+throwable.getMessage());
        throwable.printStackTrace();
    }
}
