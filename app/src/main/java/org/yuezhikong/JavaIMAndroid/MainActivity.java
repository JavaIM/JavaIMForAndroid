package org.yuezhikong.JavaIMAndroid;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
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

import org.yuezhikong.JavaIMAndroid.Fragment.CardNotice;
import org.yuezhikong.JavaIMAndroid.Fragment.CardServer;
import org.yuezhikong.JavaIMAndroid.utils.FileUtils;
import org.yuezhikong.JavaIMAndroid.utils.SavedServerFileLayout;
import org.yuezhikong.utils.SaveStackTrace;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

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

    private FragmentManager fragmentManager;
    private FragmentTransaction fragmentTransaction;

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
        findViewById(R.id.fragmentContainerView).setPadding(0,0,0,0);
        File SavedServerFile = new File(Application.getInstance().getFilesDir().getPath()+"/SavedServers.json");
        SavedServerFileLayout layout = new SavedServerFileLayout();
        Gson gson = new Gson();
        if (CreateServerList())
            CheckServerList();
        try {
            layout = gson.fromJson(FileUtils.readTxt(SavedServerFile, StandardCharsets.UTF_8).toString()
                    , SavedServerFileLayout.class);
        } catch (JsonSyntaxException | IOException e) {
            Toast.makeText(this, "无法解析保存的服务器文件，请检查文件内容", Toast.LENGTH_SHORT).show();
            return;
        }
        Fragment cardNotice = getSupportFragmentManager().findFragmentById(R.id.card_notice);
        if (cardNotice != null)
            getSupportFragmentManager().beginTransaction().remove(cardNotice).commit();
        if (!(layout.getServerInformation().isEmpty())){
            ShowServerList();
        }
    }

    @Override
    protected void onStop() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        for (Fragment fragment : getSupportFragmentManager().getFragments())
        {
            transaction.remove(fragment);
        }
        transaction.commit();
        CardServer.resetTop();
        super.onStop();
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

    public void CheckServerList(){
        File SavedServerFile = new File(Application.getInstance().getFilesDir().getPath()+"/SavedServers.json");
        SavedServerFileLayout layout;
        Gson gson = new Gson();
        try {
            layout = gson.fromJson(FileUtils.readTxt(SavedServerFile, StandardCharsets.UTF_8).toString()
                    , SavedServerFileLayout.class);
            CardNotice CardNotice = new CardNotice();
            if (layout.getServerInformation().isEmpty()){
                fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .setReorderingAllowed(true)
                        .add(R.id.fragmentContainerView, CardNotice,"Notice")
                        .commit();
            }
        } catch (JsonSyntaxException | IOException e) {
            Toast.makeText(this, "无法解析保存的服务器文件，请检查文件内容", Toast.LENGTH_SHORT).show();
        }
    }
    public void ShowServerList() {
        File SavedServerFile = new File(Application.getInstance().getFilesDir().getPath() + "/SavedServers.json");
        SavedServerFileLayout layout;
        Gson gson = new Gson();
        try {
            layout = gson.fromJson(FileUtils.readTxt(SavedServerFile, StandardCharsets.UTF_8).toString()
                    , SavedServerFileLayout.class);
        } catch (JsonSyntaxException | IOException e) {
            Toast.makeText(this, "无法解析保存的服务器文件，请检查文件内容", Toast.LENGTH_SHORT).show();
            return;
        }
        fragmentTransaction = getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true);
        for (SavedServerFileLayout.ServerInformationBean Information : layout.getServerInformation()) {
            CardServer CardServer = new CardServer();
            CardServer.setServerName(Information.getServerRemark());
            CardServer.setServerAddr(Information.getServerAddress());
            CardServer.setServerPort(Information.getServerPort());
            fragmentTransaction.add(R.id.fragmentContainerView, CardServer, "CardServer");
        }
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