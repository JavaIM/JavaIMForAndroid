package org.yuezhikong.JavaIMAndroid;

import android.util.Log;

import androidx.multidex.MultiDexApplication;

import org.jetbrains.annotations.NotNull;

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
        IOThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread newThread = new Thread(r, "IO Thread #" + threadNumber.getAndIncrement());
                newThread.setUncaughtExceptionHandler((thread, throwable) -> {
                    Log.d("JavaIM ThreadPool","线程:"+thread.getName()+"出现异常");
                    throwable.printStackTrace();
                });
                return newThread;
            }
        });
        UserRequestDisposeThreadPool = Executors.newSingleThreadExecutor(new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread newThread = new Thread(r, "User Request Dispose Thread #" + threadNumber.getAndIncrement());
                newThread.setUncaughtExceptionHandler((thread, throwable) -> {
                    Log.d("JavaIM ThreadPool","线程:"+thread.getName()+"出现异常");
                    throwable.printStackTrace();
                });
                return newThread;
            }
        });
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
