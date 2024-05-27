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
    }
}
