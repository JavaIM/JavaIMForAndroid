package org.yuezhikong.JavaIMAndroid;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.yuezhikong.JavaIMAndroid.utils.FileUtils;
import org.yuezhikong.JavaIMAndroid.utils.SavedServerFileLayout;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static boolean Session = false;
    private Client client;
    public static File UsedKey;

    /**
     * 获取会话状态
     * <p>就是是否已经连接服务器</p>
     * @return true:已连接 false:未连接
     */
    public static boolean isSession() {
        return Session;
    }

    public static String ServerAddr = "";

    public static String Servername = "";
    public static int ServerPort = 0;
    private static MainActivity Instance;

    public static MainActivity getInstance() {
        return Instance;
    }

    public void OutputToChatLog(String msg)
    {
        runOnUiThread(()-> OutputToChatLogNoRunOnUIThread(msg));
    }
    private void OutputToChatLogNoRunOnUIThread(String msg)
    {
        ((TextView)findViewById(R.id.ChatLog)).setText(String.format("%s\r\n%s",((TextView)findViewById(R.id.ChatLog)).getText().toString(),msg));
        findViewById(R.id.ScrollView).post(() -> ((ScrollView)findViewById(R.id.ScrollView)).fullScroll(ScrollView.FOCUS_DOWN));
    }
    private void ErrorOutputToUserScreen(int id)
    {
        runOnUiThread(()-> Toast.makeText(MainActivity.this,getResources().getString(id),Toast.LENGTH_LONG).show());
    }

    public void ClearScreen(View view) {
        runOnUiThread(()-> ((TextView) findViewById(R.id.ChatLog)).setText(""));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Instance = this;
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        final FloatingActionButton floatingActionButton = findViewById(R.id.create);
        floatingActionButton.setOnClickListener(this::ChangeToCreateActivity);
        File SavedServerFile = new File(Application.getInstance().getFilesDir().getPath()+"/SavedServers.json");
        SavedServerFileLayout layout = new SavedServerFileLayout();
        Gson gson = new Gson();
        try {
            layout = gson.fromJson(FileUtils.readTxt(SavedServerFile, StandardCharsets.UTF_8).toString()
                    , SavedServerFileLayout.class);
        } catch (JsonSyntaxException | IOException e) {
        Toast.makeText(this, "无法解析保存的服务器文件，请检查文件内容", Toast.LENGTH_SHORT).show();
    }
        if (CreateServerList())
            CheckServerList();
        if (!(layout.getServerInformation().isEmpty())){
            ShowServerList();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    /**
     * 初始化并创建服务器管理json
     * @return 初始化是否成功
     */
    public boolean CreateServerList(){
        File SavedServerFile = new File(Application.getInstance().getFilesDir().getPath()+"/SavedServers.json");
        SavedServerFileLayout layout;
        Gson gson = new Gson();
        try {
            if (((!SavedServerFile.exists() && !SavedServerFile.createNewFile()))){
                Toast.makeText(this, "由于出现文件系统错误，无法管理保存的服务器", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (SavedServerFile.length() == 0) {
                layout = new SavedServerFileLayout();
                layout.setVersion(ConfigFile.SavedServerFileVersion);
                layout.setServerInformation(new ArrayList<>());
                FileUtils.writeTxt(SavedServerFile,gson.toJson(layout), StandardCharsets.UTF_8);
            }
            else {
                try {
                    layout = gson.fromJson(FileUtils.readTxt(SavedServerFile, StandardCharsets.UTF_8).toString()
                            , SavedServerFileLayout.class);
                } catch (JsonSyntaxException e) {
                    Toast.makeText(this, "无法解析保存的服务器文件，请检查文件内容", Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (layout.getVersion() != ConfigFile.SavedServerFileVersion &&
                        layout.getVersion() != 1//版本1已经兼容自动升级，因此可以继续
                )
                {
                    Toast.makeText(this, "保存的服务器文件版本与程序版本不一致,操作已被取消!", Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (layout.getVersion() == 1) {//自动更新判断，后期大于3个版本后，使用switch
                    layout.setVersion(2);
                    for (SavedServerFileLayout.ServerInformationBean bean : layout.getServerInformation())
                    {
                        bean.setServerToken("");
                    }
                }
                FileUtils.writeTxt(SavedServerFile,gson.toJson(layout), StandardCharsets.UTF_8);
            }
            return true;
        } catch  (IOException e) {
            Toast.makeText(this, "出现文件系统错误，无法管理保存的服务器", Toast.LENGTH_SHORT).show();
            SaveStackTrace.saveStackTrace(e);
            return false;
        }
    }
    public static class CardNotice extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.card_notice, container, false);
        }
    }
    public static class CardServer extends Fragment {
        private String ServerName;
        private String ServerAddr;
        private int port;
        public void setServerName(String ServerName) {this.ServerName = ServerName;}
        public void setServerPort(int ServerPort) {
            this.port = ServerPort;
        }
        public void setServerAddr(String ServerAddr) {this.ServerAddr = ServerAddr;}
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.card_server, container, false);
            TextView textview = view.findViewById(R.id.ServerName);
            textview.setText(ServerName);
            textview = view.findViewById(R.id.Address);
            textview.setText(ServerAddr + ":" + port);
            return view;
        }
    }
    public void CheckServerList(){
        File SavedServerFile = new File(Application.getInstance().getFilesDir().getPath()+"/SavedServers.json");
        SavedServerFileLayout layout;
        Gson gson = new Gson();
        try {
            layout = gson.fromJson(FileUtils.readTxt(SavedServerFile, StandardCharsets.UTF_8).toString()
                    , SavedServerFileLayout.class);
            if (layout.getServerInformation().isEmpty()){
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                CardNotice CardNotice = new CardNotice();
                fragmentTransaction.add(R.id.main, CardNotice);
                fragmentTransaction.commit();
            }
        } catch (JsonSyntaxException | IOException e) {
            Toast.makeText(this, "无法解析保存的服务器文件，请检查文件内容", Toast.LENGTH_SHORT).show();
        }
    }
    public void ShowServerList() {
        File SavedServerFile = new File(Application.getInstance().getFilesDir().getPath() + "/SavedServers.json");
        SavedServerFileLayout layout;
        Gson gson = new Gson();
        SavedServerFileLayout.ServerInformationBean Information = new SavedServerFileLayout.ServerInformationBean();
        try {
            layout = gson.fromJson(FileUtils.readTxt(SavedServerFile, StandardCharsets.UTF_8).toString()
                    , SavedServerFileLayout.class);
            Information = layout.getServerInformation().get(0);
        } catch (JsonSyntaxException | IOException e) {
            Toast.makeText(this, "无法解析保存的服务器文件，请检查文件内容", Toast.LENGTH_SHORT).show();
        }
        String ServerName = Information.getServerRemark();
        String ServerAddress = Information.getServerAddress();
        int ServerPort = Information.getServerPort();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        CardServer CardServer = new CardServer();
        CardServer.setServerName(ServerName);
        CardServer.setServerAddr(ServerAddress);
        CardServer.setServerPort(ServerPort);
        fragmentTransaction.add(R.id.main, CardServer);
        fragmentTransaction.commit();
    }
    public void Connect(View view) {
        if (UsedKey == null)
        {
            ErrorOutputToUserScreen(R.string.Error5);
            return;
        }
        String IPAddress = ServerAddr;
        if (IPAddress.isEmpty())
        {
            ErrorOutputToUserScreen(R.string.Error1);
            return;
        }
        int port = ServerPort;
        if (port <= 0)
        {
            ErrorOutputToUserScreen(R.string.Error2);
        }
        if (port > 65535)
        {
            ErrorOutputToUserScreen(R.string.Error2);
        }
        if (!Session)
        {
            ((TextView)findViewById(R.id.ChatLog)).setText("");
            Session = true;
            new Thread(new ThreadGroup(Thread.currentThread().getThreadGroup(), "Client Thread Group"),"Network Thread")
            {
                @Override
                public void run() {
                    this.setUncaughtExceptionHandler((thread,throwable) -> {
                        throwable.printStackTrace();
                        OutputToChatLog("客户端已经终止了运行，因为出现了异常");
                        OutputToChatLog("详情请查看系统LogCat");
                        Session = false;
                    });
                    client = new Client(ConfigFile.PublicKeyFile,ConfigFile.PrivateKeyFile);
                    client.start(ServerAddr,ServerPort);
                }
            }.start();
        }
    }

    //用户按下发送按钮
    public void Disconnect(View view) {
        if (Session)
        {
            Session = false;
            if (!client.getClientStopStatus()) {
                client.TerminateClient();
                ClearScreen(view);
            }
        }
    }
    public void ChangeToCreateActivity(View view) {
        //开始创建新Activity过程
        Intent intent=new Intent();
        intent.setClass(MainActivity.this, CreateActivity.class);
        //设置 如果这个activity已经启动了，就不产生新的activity，而只是把这个activity实例加到栈顶
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        //启动Activity
        startActivity(intent);
    }

}