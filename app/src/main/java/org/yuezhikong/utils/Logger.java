package org.yuezhikong.utils;

import android.util.Log;

import org.yuezhikong.JavaIMAndroid.MainActivity;

public class Logger {
    public void info(String Message) {
        MainActivity.getInstance().OutputToChatLog(Message);
        Log.i("JavaIM",Message);
    }
    public void ChatMsg(String Message) { info(Message);}
    public void warning(String Message) { info(Message); }
    public void error(String Message) { info(Message); }
    public Logger(Object anObject) {}
}
