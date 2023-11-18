package org.yuezhikong.JavaIMAndroid;

import android.util.Base64;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.yuezhikong.JavaIMAndroid.JavaIM.ClientMain;
import org.yuezhikong.utils.NetworkManager;
import org.yuezhikong.utils.Protocol.NormalProtocol;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class Client extends ClientMain {
    private Thread RecvMessageThread;
    private static final ExecutorService UserRequestDisposeThreadPool = Executors.newSingleThreadExecutor(new ThreadFactory() {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(@NotNull Runnable r) {
            return new Thread(r, "User Request Dispose Thread #" + threadNumber.getAndIncrement());
        }
    });

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
        UserRequestDisposeThreadPool.execute(() -> {
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

    @Override
    public void start(String ServerAddress, int ServerPort) {
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
        MainActivity.Session = false;
        QuitReason = "用户要求关闭";
        UserRequestDisposeThreadPool.execute(() -> {
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
            synchronized (ClientExitLock) {
                ClientExitLock.notifyAll();
            }
            if (RecvMessageThread != null) {
                RecvMessageThread.interrupt();
            }
            if (getClientThreadGroup() != null) {
                getClientThreadGroup().interrupt();
            }
        });
    }
}
