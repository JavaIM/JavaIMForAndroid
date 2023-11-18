package org.yuezhikong.JavaIMAndroid;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import org.yuezhikong.JavaIMAndroid.utils.FileUtils;

import java.io.File;
import java.util.List;
import java.util.Objects;

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
        runOnUiThread(()->{
            TextView SocketDisplay = findViewById(R.id.ChatLog);
            SocketDisplay.setText("");
        });
    }
    private ActivityResultLauncher<Intent> SettingActivityResultLauncher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Instance = this;
        setContentView(R.layout.activity_main);
        SettingActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            TextView DisplayUsedKeyTextView = findViewById(R.id.DisplayUsedKey);
            if (UsedKey == null)
            {
                DisplayUsedKeyTextView.setText("目前没有存在的公钥，可在设置中导入");
            }
            else {
                DisplayUsedKeyTextView.setText(String.format("%s%s%s", getResources().getString(R.string.UsedKeyPrefix), UsedKey.getName(), getResources().getString(R.string.UsedKeySuffix)));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        TextView ChatLog = findViewById(R.id.ChatLog);
        ChatLog.setMovementMethod(ScrollingMovementMethod.getInstance());
        Button button = findViewById(R.id.button4);
        button.setOnClickListener(this::ChangeToSettingActivity);
        button = findViewById(R.id.button8);
        button.setOnClickListener(this::Send);
        button = findViewById(R.id.button2);
        button.setOnClickListener(this::Connect);
        button = findViewById(R.id.button3);
        button.setOnClickListener(this::Disconnect);
        button = findViewById(R.id.button6);
        button.setOnClickListener(this::ClearScreen);
        TextView DisplayUsedKeyTextView = findViewById(R.id.DisplayUsedKey);
        if (FileUtils.fileListOfServerPublicKey(this).length == 0) {
            UsedKey = null;
            DisplayUsedKeyTextView.setText("目前没有存在的公钥，可在设置中导入");
        } else {
            UsedKey = new File(getFilesDir().getPath() + "/ServerPublicKey/" + (FileUtils.fileListOfServerPublicKey(this)[0]));
            DisplayUsedKeyTextView.setText(String.format("%s%s%s", getResources().getString(R.string.UsedKeyPrefix), UsedKey.getName(), getResources().getString(R.string.UsedKeySuffix)));
        }
        if (Build.VERSION.SDK_INT >= 30) {//当大于等于Android 11（API 30）时读取上一次的退出原因
            ActivityManager manager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
            List<ApplicationExitInfo> exitInfo = manager.getHistoricalProcessExitReasons(getApplication().getPackageName(), 0, 1);
            if (!exitInfo.isEmpty())
            {
                ApplicationExitInfo info = exitInfo.get(0);
                Log.i("JavaIM","The Last Exit Reason:"+info.getReason());
                Log.i("JavaIM","TimeStamp:"+info.getTimestamp());
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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
                    client = new Client();
                    client.start(ServerAddr,ServerPort);
                }
            }.start();
        }
    }

    //用户按下发送按钮
    public void Send(View view) {
        EditText UserMessageText = findViewById (R.id.UserSendMessage);
        String UserMessage = UserMessageText.getText().toString();
        if (!Session)
        {
            ErrorOutputToUserScreen(R.string.Error6);
        }
        else
        {
            if (client.getNeedConsoleInput())
            {
                client.setConsoleInput(UserMessage);
                client.setNeedConsoleInput(false);
                synchronized (client.getConsoleInputLock()) {
                    client.getConsoleInputLock().notifyAll();
                }
                return;
            }
            client.MessageSendToServer(UserMessage);
            UserMessageText.setText("");
        }
    }
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
    public void ChangeToSettingActivity(View view) {
        String tmpServerAddr;
        int tmpServerPort;
        //处理当前已经记录的Addr和Port
        tmpServerAddr = Objects.requireNonNullElse(ServerAddr, "");
        tmpServerPort = ServerPort;
        //开始创建新Activity过程
        Intent intent=new Intent();
        intent.setClass(MainActivity.this, SettingActivity.class);
        //开始向新Activity发送老Addr和Port，以便填充到编辑框
        Bundle bundle = new Bundle();
        bundle.putString("ServerAddr",tmpServerAddr);
        bundle.putInt("ServerPort",tmpServerPort);
        //从Bundle put到intent
        intent.putExtras(bundle);
        //设置 如果这个activity已经启动了，就不产生新的activity，而只是把这个activity实例加到栈顶
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        //启动Activity
        SettingActivityResultLauncher.launch(intent);
    }

}