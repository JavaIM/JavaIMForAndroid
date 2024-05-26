package org.yuezhikong.JavaIMAndroid;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.yuezhikong.Client;
import org.yuezhikong.JavaIMAndroid.utils.FileUtils;
import org.yuezhikong.JavaIMAndroid.utils.NetworkHelper;
import org.yuezhikong.Protocol.ChatProtocol;
import org.yuezhikong.Protocol.GeneralProtocol;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.channel.Channel;

public class HomeFragment extends Fragment {
    public static SavedServer.Server UseServer;
    static boolean Session = false;
    private boolean StartComplete = false;
    private AndroidClient client;

    /**
     * 打印到聊天日志(支持富文本)
     * @param msg 要打印的消息
     */
    public void OutputToChatLog(CharSequence msg) {
        requireActivity().runOnUiThread(() -> {
            TextView view = requireActivity().findViewById(R.id.ChatLog);
            RichTextTextViewWrite(msg,view);
            if (getActivity() == null)
                return;
            requireActivity().findViewById(R.id.ScrollView).post(() -> ((ScrollView) requireActivity().findViewById(R.id.ScrollView)).fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

    /**
     * 追加富文本消息
     * @param msg 消息
     * @param targetTextView 目标textview
     */
    private void RichTextTextViewWrite(CharSequence msg, @NonNull TextView targetTextView)
    {
        if (targetTextView.getText() == null || targetTextView.getText().equals("") || !(targetTextView.getText() instanceof SpannableString))
            targetTextView.setText(new SpannableString(""));
        SpannableStringBuilder builder = new SpannableStringBuilder(targetTextView.getText());
        builder.append(msg).append("\n");
        targetTextView.setText(builder);
    }
    private void ErrorOutputToUserScreen(int id)
    {
        requireActivity().runOnUiThread(()-> Toast.makeText(requireActivity(),getResources().getString(id),Toast.LENGTH_LONG).show());
    }

    private void ClearScreen(View view) {
        ClearScreen();
    }
    public void ClearScreen() {
        requireActivity().runOnUiThread(()-> ((TextView) requireActivity().findViewById(R.id.ChatLog)).setText(""));
        requireActivity().runOnUiThread(()-> ((TextView) requireActivity().findViewById(R.id.ChatLog)).setText(""));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        ((TextView) requireActivity().findViewById(R.id.ChatLog)).setMovementMethod(ScrollingMovementMethod.getInstance());
        requireActivity().findViewById(R.id.button8).setOnClickListener(this::Send);
        requireActivity().findViewById(R.id.button2).setOnClickListener(this::Connect);
        requireActivity().findViewById(R.id.button3).setOnClickListener(this::Disconnect);
        requireActivity().findViewById(R.id.button6).setOnClickListener(this::ClearScreen);
    }

    @Override
    public void onResume() {
        super.onResume();
        new Thread(() -> {
            ((Runnable) () -> {
                File servers = new File(requireActivity().getFilesDir(),"servers.json");

                SavedServer savedServers;
                Gson gson = new Gson();
                try {
                    savedServers = gson.fromJson(FileUtils.readTxt(servers, StandardCharsets.UTF_8), SavedServer.class);
                } catch (Throwable t) {
                    return;
                }
                for (SavedServer.Server server : savedServers.getServers())
                {
                    if (server.isIsUsingServer())
                    {
                        HomeFragment.UseServer = server;
                        return;
                    }
                }
            }).run();
            requireActivity().runOnUiThread(() -> {
                final TextView DisplayUseCACertsTextView = requireActivity().findViewById(R.id.DisplayUseCACert);
                if (UseServer == null) {
                    DisplayUseCACertsTextView.setText("目前没有使用的服务器，可在设置中选择/导入");
                } else {
                    DisplayUseCACertsTextView.setText(String.format("当前使用的服务器:%s",UseServer.getServerName()));
                    if (!UseServer.getServerLoginToken().isEmpty() && !Session)
                        TryTokenLogin(UseServer.getServerAddress(), UseServer.getServerPort());
                }
            });
        }, "Start process Thread").start();
    }

    public void Connect(View view) {
        if (client != null && client.getChannel() != null && client.getChannel().isOpen()) {
            Toast.makeText(requireActivity(), "客户端正在运行中!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (UseServer == null)
        {
            Toast.makeText(requireActivity(), "没有正在使用的服务器", Toast.LENGTH_SHORT).show();
            return;
        }
        if (UseServer.getX509CertContent() == null || UseServer.getX509CertContent().isEmpty())
        {
            ErrorOutputToUserScreen(R.string.Error5);
            return;
        }
        String IPAddress = UseServer.getServerAddress();
        if (IPAddress.isEmpty())
        {
            ErrorOutputToUserScreen(R.string.Error1);
            return;
        }
        int port = UseServer.getServerPort();
        if (port <= 0 || port > 65535)
        {
            ErrorOutputToUserScreen(R.string.Error2);
            return;
        }
        if (!UseServer.getServerLoginToken().isEmpty())
        {
            TryTokenLogin(IPAddress, port);
            return;
        }
        final EditText UserName = new EditText(requireActivity());
        new AlertDialog.Builder(requireActivity())
                .setTitle("请输入用户名").setView(UserName)
                .setNegativeButton("取消启动", (dialog,which) -> dialog.cancel())
                .setPositiveButton("确定", (dialog, which) -> {
                    // 用户名
                    String Username = UserName.getText().toString();
                    EditText Passwd = new EditText(requireActivity());
                    new AlertDialog.Builder(requireActivity())
                            .setTitle("请输入密码").setView(Passwd)
                            .setNegativeButton("取消启动", (Dialog,Which) -> Dialog.cancel())
                            .setPositiveButton("确定",(Dialog,Which) -> {
                                String passwd = Passwd.getText().toString();
                                ConnectToServer0(IPAddress,port,UseServer.getX509CertContent(),Username,passwd);
                            }).show();
                }).show();

    }

    private void TryTokenLogin(String IPAddress, int port) {
        if (Session)
        {
            Toast.makeText(requireActivity(), "客户端正在运行中", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(requireActivity(), "正在自动登录,如果登录异常,请前往设置清除token", Toast.LENGTH_SHORT).show();
        ConnectToServer0(IPAddress,port,UseServer.getX509CertContent(),"","");
        OutputToChatLog(getRichText("自动登录应该已经完成,如果登录异常,请前往设置清除token",Color.parseColor("#00FF00")));
    }

    private class AndroidClient extends Client
    {
        public Channel getChannel()
        {
            return channel;
        }
        @Override
        protected ThreadFactory getWorkerThreadFactory() {
            return new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                private final ThreadGroup IOThreadGroup = new ThreadGroup(getClientThreadGroup(), "IO Thread Group");

                @Override
                public Thread newThread(@NotNull Runnable r) {
                    Thread newThread = new Thread(IOThreadGroup,
                            () -> {
                                try {
                                    r.run();
                                } catch (Throwable throwable) {
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
                            }, "Netty Worker Thread #" + threadNumber.getAndIncrement());
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

        @Override
        protected void DisplayChatMessage(String sourceUserName, String message) {
            NormalPrint(String.format("[%s] [%s]:%s",
                    SimpleDateFormat.getDateInstance().format(System.currentTimeMillis()),
                    sourceUserName,
                    message));
        }

        @Override
        protected void DisplayMessage(String message) {
            NormalPrint(String.format("[%s] [系统消息]:%s",
                    SimpleDateFormat.getDateInstance().format(System.currentTimeMillis()),
                    message));
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
            return UseServer.getServerLoginToken();
        }

        @Override
        protected void setToken(String newToken) {
            Gson gson = new Gson();
            UseServer.setServerLoginToken(newToken);
            File ServerList = new File(requireActivity().getFilesDir(),"servers.json");
            try {
                SavedServer savedServer = gson.fromJson(FileUtils.readTxt(ServerList, StandardCharsets.UTF_8), SavedServer.class);
                for (SavedServer.Server server : savedServer.getServers())
                {
                    if (server.getServerName().equals(UseServer.getServerName()))
                    {
                        server.setServerLoginToken(newToken);
                        break;
                    }
                }
                FileUtils.writeTxt(ServerList, gson.toJson(savedServer), StandardCharsets.UTF_8);
            } catch (IOException e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);

                ErrorPrint("自动登录Token写入失败");
                ErrorPrint(sw.toString());
                pw.close();

                Toast.makeText(requireActivity(), "自动登录Token写入失败!", Toast.LENGTH_SHORT).show();
            }
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
    private void ConnectToServer0(String ServerAddress, int Port, String ServerCACert , String UserName, String Passwd)
    {
        if (!NetworkHelper.isNetworkCanUse(requireActivity()))
        {
            Toast.makeText(requireActivity(), "当前无网络可用!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (NetworkHelper.isCellular(requireActivity()))
            Toast.makeText(requireActivity(), "您正在使用移动数据网络，请注意流量消耗", Toast.LENGTH_SHORT).show();
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
            try (ByteArrayInputStream stream = new ByteArrayInputStream(ServerCACert.getBytes(StandardCharsets.UTF_8))){
                CertificateFactory factory = CertificateFactory.getInstance("X.509","BC");
                ServerCARootCert = (X509Certificate) factory.generateCertificate(stream);
            } catch (CertificateException | NoSuchProviderException | IOException e) {
                Session = false;
                StartComplete = false;

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);

                OutputToChatLog(getRichText(sw.toString(), Color.RED));
                Log.e("JavaIM Client", sw.toString());
                UserNetworkRequestThreadPool.shutdownNow();
                group.interrupt();
                return;
            }
            new Thread(group,"Client Thread")
            {
                @Override
                public void run() {
                    this.setUncaughtExceptionHandler((thread,throwable) -> {
                        OutputToChatLog(getRichText("客户端已经终止了运行，因为出现了异常", Color.parseColor("#00CCFF")));
                        Session = false;
                        StartComplete = false;
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        throwable.printStackTrace(pw);
                        OutputToChatLog(getRichText(sw.toString(),Color.RED));
                        pw.close();
                        UserNetworkRequestThreadPool.shutdownNow();
                        group.interrupt();
                    });
                    client = new AndroidClient(group);
                    try {
                        client.start(ServerAddress, Port, ServerCARootCert, UserName, Passwd);
                    } catch (Throwable throwable) {
                        OutputToChatLog(getRichText("客户端已经终止了运行，因为出现了异常", Color.parseColor("#00CCFF")));
                        Session = false;
                        StartComplete = false;
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        throwable.printStackTrace(pw);
                        OutputToChatLog(getRichText(sw.toString(),Color.RED));
                        pw.close();
                        UserNetworkRequestThreadPool.shutdownNow();
                        group.interrupt();
                        return;
                    }

                    OutputToChatLog(getRichText("客户端进程已经结束...", Color.parseColor("#00CCFF")));
                    Session = false;
                    StartComplete = false;
                    UserNetworkRequestThreadPool.shutdownNow();
                    group.interrupt();
                }
            }.start();
        }
        else {
            Toast.makeText(requireActivity(), "客户端正在运行中!", Toast.LENGTH_SHORT).show();
        }
    }

    //用户按下发送按钮
    public void Send(View view) {
        final EditText UserMessageText = requireActivity().findViewById (R.id.UserSendMessage);
        String UserMessage = UserMessageText.getText().toString();
        UserMessageText.setText("");
        if (!Session)
        {
            ErrorOutputToUserScreen(R.string.Error6);
        }
        else
        {
            if (StartComplete)
            {
                UserNetworkRequestThreadPool.execute(() -> {
                    ChatProtocol userInput = new ChatProtocol();
                    userInput.setMessage(UserMessage);

                    GeneralProtocol generalProtocol = new GeneralProtocol();
                    generalProtocol.setProtocolData(client.getGson().toJson(userInput));
                    generalProtocol.setProtocolVersion(client.getProtocolVersion());
                    generalProtocol.setProtocolName("ChatProtocol");

                    client.SendData(client.getGson().toJson(generalProtocol));
                });
            }
            else {
                Toast.makeText(requireActivity(), "客户端尚未启动完毕", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public void Disconnect(View view) {
        if (Session)
        {
            if (StartComplete)
                client.disconnect();
            else {
                Toast.makeText(requireActivity(), "客户端启动尚未完全完成，正在强制终止", Toast.LENGTH_SHORT).show();
                client.getClientThreadGroup().interrupt();
                Session = false;
            }
        }
    }

}