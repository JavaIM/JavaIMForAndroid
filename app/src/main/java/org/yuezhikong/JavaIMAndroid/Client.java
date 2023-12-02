package org.yuezhikong.JavaIMAndroid;

import android.util.Base64;
import android.widget.Toast;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.yuezhikong.JavaIMAndroid.JavaIM.ClientMain;
import org.yuezhikong.utils.NetworkManager;
import org.yuezhikong.utils.Protocol.NormalProtocol;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class Client extends ClientMain {
    private Thread RecvMessageThread;

    /**
     * 请求用户token
     * @apiNote Token系统在Android暂时停用！返回永远为空字符串
     * @return 用户token
     */
    @Override
    protected String RequestUserToken() {
        return "";
    }

    @Override
    protected File getPublicKeyFile() {
        return PublicKeyFile;
    }

    @Override
    protected File getPrivateKeyFile() {
        return PrivateKeyFile;
    }

    private final File PublicKeyFile;
    private final File PrivateKeyFile;

    public Client(File publicKeyFile, File privateKeyFile)
    {
        PublicKeyFile = Objects.requireNonNull(publicKeyFile);
        PrivateKeyFile = Objects.requireNonNull(privateKeyFile);
    }

    /**
     * 写入用户token
     * @apiNote Token系统在Android暂时停用！调用此方法无意义
     */
    @Override
    protected void writeUserToken(String UserToken) {

    }

    public boolean getNeedConsoleInput()
    {
        return needConsoleInput;
    }
    public void setNeedConsoleInput(boolean input)
    {
        needConsoleInput = input;
    }

    @Override
    protected File getServerPublicKeyFile() {
        return MainActivity.UsedKey;
    }

    @Override
    protected synchronized ScheduledExecutorService getTimerThreadPool() {
        if (TimerThreadPool == null)
        {
            TimerThreadPool = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);

                @Override
                public Thread newThread(@NotNull Runnable r) {
                    Thread newThread = new Thread(getClientThreadGroup(),r, "Timer Thread #" + threadNumber.getAndIncrement());
                    newThread.setUncaughtExceptionHandler(new NonCrashThreadUncaughtExceptionHandler());
                    return newThread;
                }
            });
        }
        return TimerThreadPool;
    }

    private final Object ClientExitLock = new Object();
    private final Object ClientStartLock = new Object();
    private volatile boolean ClientStartStatus = false;
    @Override
    protected void SendMessage() {
        ClientStartStatus = true;
        synchronized (ClientStartLock) {
            ClientStartLock.notifyAll();
        }
        synchronized (ClientExitLock)
        {
            try {
                ClientExitLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void setConsoleInput(String data)
    {
        ConsoleInput = data;
    }
    public Object getConsoleInputLock()
    {
        return ConsoleInputLock;
    }

    @Override
    protected String[] RequestUserNameAndPassword() {
        getLogger().info("请输入用户名");
        needConsoleInput = true;
        synchronized (getConsoleInputLock())
        {
            try {
                getConsoleInputLock().wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        String UserName = ConsoleInput;
        getLogger().info("请输入密码");
        ConsoleInput = "";
        needConsoleInput = true;
        synchronized (getConsoleInputLock())
        {
            try {
                getConsoleInputLock().wait();
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        String Password = ConsoleInput;
        ConsoleInput = "";
        return new String[] { UserName, Password };
    }

    public void MessageSendToServer(String Message) {
        Application.getInstance().getUserRequestDisposeThreadPool().execute(() -> {
            if (!ClientStartStatus)
            {
                synchronized (ClientStartLock)
                {
                    if (!ClientStartStatus)
                    {
                        try {
                            ClientStartLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            NormalProtocol protocol = new NormalProtocol();
            NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
            head.setVersion(ConfigFile.ProtocolVersion);
            head.setType("Chat");
            protocol.setMessageHead(head);

            NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
            body.setMessage(Message);
            protocol.setMessageBody(body);

            try {
                NetworkManager.WriteDataToRemote(getClientNetworkData(), Base64.encodeToString(getAes().encrypt(new Gson().toJson(protocol)),Base64.NO_WRAP));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private Thread mainThread;
    @Override
    public void start(String ServerAddress, int ServerPort) {
        mainThread = Thread.currentThread();
        new Thread(mainThread.getThreadGroup(),"waitInterrupt Thread")
        {
            @Override
            public void run() {
                this.setUncaughtExceptionHandler(new NonCrashThreadUncaughtExceptionHandler());
                synchronized (this)
                {
                    try {
                        this.wait();
                    } catch (InterruptedException ignored) {}
                }
                MainActivity.Session = false;
                synchronized (ClientExitLock) {
                    ClientExitLock.notifyAll();
                }
            }
        }.start();
        super.SpecialMode = true;
        super.start(ServerAddress, ServerPort);
    }

    protected void StartRecvMessageThread() {
        RecvMessageThread = new Thread()
        {
            public Thread start2() {
                start();
                return this;
            }

            @Override
            public void run() {
                this.setName("RecvMessage Thread");
                Client.super.StartRecvMessageThread();
            }

        }.start2();
    }

    public void TerminateClient() {
        QuitReason = "用户要求关闭";
        MainActivity.getInstance().runOnUiThread(() -> Toast.makeText(MainActivity.getInstance(),
                "正在等待程序结束...最长可能需要5秒钟", Toast.LENGTH_SHORT).show());
        Application.getInstance().getUserRequestDisposeThreadPool().execute(() -> {
            if (getClientNetworkData() != null) {
                Gson gson = new Gson();
                NormalProtocol protocol = new NormalProtocol();
                NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                head.setVersion(ConfigFile.ProtocolVersion);
                head.setType("Leave");
                protocol.setMessageHead(head);
                NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
                body.setMessage(".quit");
                body.setFileLong(0);
                protocol.setMessageBody(body);
                try {
                    NetworkManager.WriteDataToRemote(getClientNetworkData(), Base64.encodeToString(getAes().encrypt(gson.toJson(protocol)), Base64.NO_WRAP));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (RecvMessageThread != null) {
                RecvMessageThread.interrupt();
            }
            if (mainThread != null) {
                mainThread.interrupt();
            }
        });
    }
}
