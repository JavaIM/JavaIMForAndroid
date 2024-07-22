package org.yuezhikong.JavaIMAndroid;

import android.widget.Toast;

import androidx.multidex.MultiDexApplication;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

public class Application extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        // 设置 JCE Provider
        Security.removeProvider("BC");
        Security.addProvider(new BouncyCastleProvider());
        // 设置默认崩溃处理器
        Thread.setDefaultUncaughtExceptionHandler(new org.yuezhikong.JavaIMAndroid.utils.NonCrashThreadUncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
                super.uncaughtException(thread,throwable);
                Toast.makeText(this,"程序遇到致命错误:"+throwable.getMessage(), Toast.LENGTH_LONG).show();
            }
        })
    }
}
