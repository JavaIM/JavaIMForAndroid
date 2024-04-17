package org.yuezhikong.JavaIMAndroid;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.multidex.MultiDexApplication;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jetbrains.annotations.NotNull;

import java.security.Security;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class Application extends MultiDexApplication {
    private ExecutorService IOThreadPool;
    private ExecutorService UserRequestDisposeThreadPool;
    private static Application instance;

    public static Application getInstance() {
        return instance;
    }

    public Application()
    {
        instance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 设置 JCE Provider
        Security.removeProvider("BC");
        Security.addProvider(new BouncyCastleProvider());
        // 初始化各种pool
        IOThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread newThread = new Thread(r, "IO Thread #" + threadNumber.getAndIncrement());
                newThread.setUncaughtExceptionHandler(new NonCrashThreadUncaughtExceptionHandler());
                return newThread;
            }
        });
        UserRequestDisposeThreadPool = Executors.newSingleThreadExecutor(new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread newThread = new Thread(r, "User Request Dispose Thread #" + threadNumber.getAndIncrement());
                newThread.setUncaughtExceptionHandler(new NonCrashThreadUncaughtExceptionHandler());
                return newThread;
            }
        });
        if (Build.VERSION.SDK_INT >= 30) {//当大于等于Android 11（API 30）时读取上一次的退出原因
            ActivityManager manager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
            List<ApplicationExitInfo> exitInfo = manager.getHistoricalProcessExitReasons(getPackageName(), 0, 1);
            if (!exitInfo.isEmpty())
            {
                ApplicationExitInfo info = exitInfo.get(0);
                Log.i("JavaIM","The Last Exit Reason:"+info.getReason());
                Log.i("JavaIM","TimeStamp:"+info.getTimestamp());
            }
        }
    }

    public ExecutorService getIOThreadPool() {
        return IOThreadPool;
    }

    public ExecutorService getUserRequestDisposeThreadPool() {
        return UserRequestDisposeThreadPool;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        IOThreadPool.shutdownNow();
        UserRequestDisposeThreadPool.shutdownNow();
    }
}
