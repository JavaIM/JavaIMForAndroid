package org.yuezhikong.JavaIMAndroid;

import java.io.File;

public class ConfigFile {
    //客户端公钥文件
    public static final File PublicKeyFile = new File(Application.getInstance().getFilesDir().getAbsolutePath()+
            "/ClientRSAKeys/Public.txt");
    //客户端私钥文件
    public static final File PrivateKeyFile = new File(Application.getInstance().getFilesDir().getAbsolutePath()+
            "/ClientRSAKeys/Private.txt");
    //协议版本
    public static final int ProtocolVersion = 7;
    //心跳包时间间隔，单位为：秒
    public static int HeartbeatInterval = 30;
    public static boolean AllowedTransferProtocol = true;
    public static final int SavedServerFileVersion = 1;
}
