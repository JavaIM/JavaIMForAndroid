package org.yuezhikong.JavaIMAndroid.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.CallSuper;

public class NonCrashThreadUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler{
    @CallSuper
    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
        Log.d("JavaIM Thread",thread.getName()+"出现错误"+throwable.getMessage());
        throwable.printStackTrace();
    }
}
