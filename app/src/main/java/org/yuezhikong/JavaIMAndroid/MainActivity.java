package org.yuezhikong.JavaIMAndroid;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;

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
        ((ScrollView)findViewById(R.id.ScrollView)).post(() -> ((ScrollView)findViewById(R.id.ScrollView)).fullScroll(ScrollView.FOCUS_DOWN));
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
        FloatingActionButton floatingActionButton = findViewById(R.id.create);
        floatingActionButton.setOnClickListener(this::ChangeToCreateActivity);
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
            new Thread()
            {
                @Override
                public void run() {
                    this.setName("Network Thread");
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
            if (client.RequestUserNameAndPassword)
            {
                if ("".equals(client.UserName))
                {
                    client.UserName = UserMessage;
                    OutputToChatLogNoRunOnUIThread("请输入密码：");
                    return;
                }
                client.Password = UserMessage;
                client.RequestUserNameAndPassword = false;
                synchronized (client.lock) {
                    client.lock.notifyAll();
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
            client.TerminateClient();
            ClearScreen(view);
        }
    }
    public void ChangeToCreateActivity(View view) {
        //开始创建新Activity过程
        Intent intent=new Intent();
        intent.setClass(MainActivity.this, CreateActivity.class);
        //设置 如果这个activity已经启动了，就不产生新的activity，而只是把这个activity实例加到栈顶
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        //启动Activity
        SettingActivityResultLauncher.launch(intent);
    }

}