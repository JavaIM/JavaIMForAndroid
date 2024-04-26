package org.yuezhikong.JavaIMAndroid;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.yuezhikong.Client;
import org.yuezhikong.JavaIMAndroid.utils.FileUtils;
import org.yuezhikong.JavaIMAndroid.utils.NetworkHelper;
import org.yuezhikong.Protocol.GeneralProtocol;
import org.yuezhikong.Protocol.NormalProtocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {
    static boolean Session = false;
    private boolean StartComplete = false;
    private AndroidClient client;

    public static File UseCACert;

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

    /**
     * 打印到聊天日志(支持富文本)
     * @param msg 要打印的消息
     */
    public void OutputToChatLog(CharSequence msg) {
        runOnUiThread(() -> {
            TextView view = findViewById(R.id.ChatLog);
            RichTextTextViewWrite(msg,view);
            findViewById(R.id.ScrollView).post(() -> ((ScrollView) findViewById(R.id.ScrollView)).fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

    /**
     * 追加富文本消息
     * @param msg 消息
     * @param targetTextView 目标textview
     */
    private void RichTextTextViewWrite(CharSequence msg, TextView targetTextView)
    {
        if (targetTextView.getText() == null || targetTextView.getText().equals("") || !(targetTextView.getText() instanceof SpannableString))
            targetTextView.setText(new SpannableString(""));
        SpannableStringBuilder builder = new SpannableStringBuilder((SpannableString) targetTextView.getText());
        builder.append(msg).append("\n");
        targetTextView.setText(builder);
    }
    private void ErrorOutputToUserScreen(int id)
    {
        runOnUiThread(()-> Toast.makeText(this,getResources().getString(id),Toast.LENGTH_LONG).show());
    }

    private void ClearScreen(View view) {
        ClearScreen();
    }
    public void ClearScreen() {
        runOnUiThread(()-> ((TextView)findViewById(R.id.ChatLog)).setText(""));
    }
    private ActivityResultLauncher<Intent> SettingActivityResultLauncher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SettingActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            final TextView DisplayUseCACertsTextView = findViewById(R.id.DisplayUseCACert);
            if (UseCACert == null)
            {
                DisplayUseCACertsTextView.setText("目前没有存在的CA证书，可在设置中导入");
            }
            else {
                DisplayUseCACertsTextView.setText(String.format(getResources().getString(R.string.UseCACertTip), UseCACert.getName()));
            }
        });
        ((TextView) findViewById(R.id.ChatLog)).setMovementMethod(ScrollingMovementMethod.getInstance());
        findViewById(R.id.button4).setOnClickListener(this::ChangeToSettingActivity);
        findViewById(R.id.button8).setOnClickListener(this::Send);
        findViewById(R.id.button2).setOnClickListener(this::Connect);
        findViewById(R.id.button3).setOnClickListener(this::Disconnect);
        findViewById(R.id.button6).setOnClickListener(this::ClearScreen);
        final TextView DisplayUseCACertsTextView = findViewById(R.id.DisplayUseCACert);
        if (FileUtils.fileListOfServerCACerts(this).length == 0) {
            UseCACert = null;
            DisplayUseCACertsTextView.setText("目前没有存在的CA证书，可在设置中导入");
        } else {
            UseCACert = new File(getFilesDir().getPath() + "/ServerCACerts/" + (FileUtils.fileListOfServerCACerts(this)[0]));
            DisplayUseCACertsTextView.setText(String.format(getResources().getString(R.string.UseCACertTip), UseCACert.getName()));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    public void Connect(View view) {
        if (!NetworkHelper.isNetworkCanUse(this))
        {
            Toast.makeText(this, "当前无网络可用!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (NetworkHelper.isCellular(this))
            Toast.makeText(this, "您正在使用移动数据网络，请注意流量消耗", Toast.LENGTH_SHORT).show();
        if (UseCACert == null)
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
        if (port <= 0 || port > 65535)
        {
            ErrorOutputToUserScreen(R.string.Error2);
            return;
        }
        final EditText UserName = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle("请输入用户名").setView(UserName)
                .setPositiveButton("确定", (dialog, which) -> {
                    // 用户名
                    String Username = UserName.getText().toString();
                    EditText Passwd = new EditText(this);
                    new AlertDialog.Builder(this)
                            .setTitle("请输入密码").setView(Passwd)
                            .setPositiveButton("确定",(Dialog,Which) -> {
                                String passwd = Passwd.getText().toString();
                                ConnectToServer0(ServerAddr,ServerPort,UseCACert,Username,passwd);
                            }).show();
                }).show();

    }

    private class AndroidClient extends Client
    {
        @Override
        protected ThreadFactory getWorkerThreadFactory() {
            return new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                private final ThreadGroup IOThreadGroup = new ThreadGroup(getClientThreadGroup(), "IO Thread Group");

                @Override
                public Thread newThread(@NotNull Runnable r) {
                    Thread newThread = new Thread(IOThreadGroup,
                            r,"Netty Worker Thread #"+threadNumber.getAndIncrement());
                    newThread.setUncaughtExceptionHandler(new NonCrashThreadUncaughtExceptionHandler()
                    {
                        @Override
                        public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
                            super.uncaughtException(thread, throwable);
                            OutputToChatLog("客户端已经终止了运行，因为出现了异常");
                            StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw);
                            throwable.printStackTrace(pw);
                            OutputToChatLog(sw.toString());
                            Session = false;
                            StartComplete = false;
                            UserNetworkRequestThreadPool.shutdownNow();
                            getClientThreadGroup().interrupt();
                        }
                    });
                    return newThread;
                }
            };
        }

        private final Gson publicGson = new Gson();

        public Gson getGson() {
            return publicGson;
        }

        /**
         * 获取协议版本
         * @return 协议版本
         */
        public int getProtocolVersion()
        {
            return protocolVersion;
        }

        @Override
        public void disconnect() {
            super.disconnect();
            Session = false;
            StartComplete = false;
            UserNetworkRequestThreadPool.shutdownNow();
            getClientThreadGroup().interrupt();
        }

        private final ThreadGroup ClientThreadGroup;
        public AndroidClient(ThreadGroup ClientThreadGroup)
        {
            this.ClientThreadGroup = ClientThreadGroup;
        }
        private ThreadGroup getClientThreadGroup() {
            return ClientThreadGroup;
        }

        @Override
        protected void onClientLogin() {
            StartComplete = true;
        }

        @Override
        protected String getToken() {
            // 暂不支持
            return "";
        }

        @Override
        protected void setToken(String newToken) {
            // 暂不支持
        }

        @Override
        protected void NormalPrint(String data) {
            OutputToChatLog(data);
        }

        @Override
        protected void NormalPrintf(String data, Object... args) {
            OutputToChatLog(String.format(data,args));
        }

        @Override
        protected void ErrorPrint(String data) {
            OutputToChatLog(getRichText(data, Color.RED));
        }

        @Override
        protected void ErrorPrintf(String data, Object... args) {
            String FormattedString = String.format(data,args);
            OutputToChatLog(getRichText(FormattedString, Color.RED));
        }
    }

    private SpannableString getRichText(String Text, int color)
    {
        ForegroundColorSpan colorSpan = new ForegroundColorSpan(color);
        SpannableString string = new SpannableString(Text);
        string.setSpan(colorSpan,0,string.length(),SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        return string;
    }

    private ExecutorService UserNetworkRequestThreadPool;
    /**
     * 连接到服务器
     * @param ServerAddress 服务器地址
     * @param Port 端口
     * @param ServerCACert 服务器CA证书
     * @param UserName 用户名
     * @param Passwd 密码
     */
    private void ConnectToServer0(String ServerAddress, int Port, File ServerCACert , String UserName, String Passwd)
    {
        if (!Session)
        {
            ClearScreen();

            Session = true;
            ThreadGroup group = new ThreadGroup(Thread.currentThread().getThreadGroup(), "Client Thread Group");
            UserNetworkRequestThreadPool = Executors.newSingleThreadExecutor(new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread newThread = new Thread(group,
                            r,"User Request Thread #"+threadNumber.getAndIncrement());
                    newThread.setUncaughtExceptionHandler(new NonCrashThreadUncaughtExceptionHandler()
                    {
                        @Override
                        public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
                            super.uncaughtException(thread, throwable);
                            OutputToChatLog("客户端已经终止了运行，因为出现了异常");
                            StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw);
                            throwable.printStackTrace(pw);
                            OutputToChatLog(sw.toString());
                            Session = false;
                            StartComplete = false;
                            UserNetworkRequestThreadPool.shutdownNow();
                            group.interrupt();
                        }
                    });
                    return newThread;
                }
            });
            X509Certificate ServerCARootCert;
            try (FileInputStream stream = new FileInputStream(ServerCACert)){
                CertificateFactory factory = CertificateFactory.getInstance("X.509","BC");
                ServerCARootCert = (X509Certificate) factory.generateCertificate(stream);
            } catch (CertificateException | NoSuchProviderException | IOException e) {
                throw new RuntimeException("Failed to open X.509 CA Cert & X.509 RSA Private key, Permission denied?",e);
            }
            new Thread(group,"Client Thread")
            {
                @Override
                public void run() {
                    this.setUncaughtExceptionHandler((thread,throwable) -> {
                        throwable.printStackTrace();
                        OutputToChatLog("客户端已经终止了运行，因为出现了异常");
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        throwable.printStackTrace(pw);
                        OutputToChatLog(sw.toString());
                        Session = false;
                        StartComplete = false;
                        UserNetworkRequestThreadPool.shutdownNow();
                        group.interrupt();
                    });
                    client = new AndroidClient(group);
                    client.start(ServerAddress, Port, ServerCARootCert, UserName, Passwd);


                    OutputToChatLog(getRichText("客户端进程已经结束...", Color.parseColor("#00CCFF")));
                    Session = false;
                    StartComplete = false;
                    UserNetworkRequestThreadPool.shutdownNow();
                    group.interrupt();
                }
            }.start();
        }
        else {
            Toast.makeText(this, "客户端正在运行中!", Toast.LENGTH_SHORT).show();
        }
    }

    //用户按下发送按钮
    public void Send(View view) {
        final EditText UserMessageText = findViewById (R.id.UserSendMessage);
        String UserMessage = UserMessageText.getText().toString();
        if (!Session)
        {
            ErrorOutputToUserScreen(R.string.Error6);
        }
        else
        {
            if (StartComplete)
            {
                UserNetworkRequestThreadPool.execute(() -> {
                    NormalProtocol userInput = new NormalProtocol();
                    userInput.setType("Chat");
                    userInput.setMessage(UserMessage);

                    GeneralProtocol generalProtocol = new GeneralProtocol();
                    generalProtocol.setProtocolData(client.getGson().toJson(userInput));
                    generalProtocol.setProtocolVersion(client.getProtocolVersion());
                    generalProtocol.setProtocolName("NormalProtocol");

                    client.SendData(client.getGson().toJson(generalProtocol));
                });
            }
            else {
                Toast.makeText(this, "客户端尚未启动完毕", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public void Disconnect(View view) {
        if (Session)
        {
            Session = false;
            client.disconnect();
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