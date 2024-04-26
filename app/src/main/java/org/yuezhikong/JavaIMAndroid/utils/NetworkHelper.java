package org.yuezhikong.JavaIMAndroid.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.util.Log;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class NetworkHelper {

    /**
     * 工具类不得实例化
     */
    private NetworkHelper() {}

    /**
     * 判断当前是否可以连接到互联网
     * @param context Android 上下文
     * @return 是/否
     */
    @Contract(pure = true)
    public static boolean isNetworkCanUse(@NotNull Context context)
    {
        Log.i("NetworkHelper","Begin to check network can use");
        // 获取当前网络管理器
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // 判断是否大于 Android SDK 23
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        }
        else {
            NetworkInfo currentNetworkInfo = connectivityManager.getActiveNetworkInfo();

            // 判断是否未连接网络或无法访问互联网
            // 阻止Deprecated警告，因为版本低于Android SDK 23
            //noinspection deprecation
            return currentNetworkInfo != null && currentNetworkInfo.getState() == NetworkInfo.State.CONNECTED;
        }
    }

    /**
     * 判断当前网络是否是数据流量
     * @param context Android 上下文
     * @return 是/否
     */
    @Contract(pure = true)
    public static boolean isCellular(@NotNull Context context)
    {
        Log.i("NetworkHelper","Begin to check network transport are cellular");
        // 获取当前网络管理器
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // 判断是否大于 Android SDK 23
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        }
        else {
            NetworkInfo currentNetworkInfo = connectivityManager.getActiveNetworkInfo();

            // 判断是否未连接网络或无法访问互联网·
            return currentNetworkInfo != null && currentNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE;
        }
    }
}
