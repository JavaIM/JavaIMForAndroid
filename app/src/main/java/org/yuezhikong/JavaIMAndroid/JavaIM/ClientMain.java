/*
 * Simplified Chinese (简体中文)
 *
 * 版权所有 (C) 2023 QiLechan <qilechan@outlook.com> 和本程序的贡献者
 *
 * 本程序是自由软件：你可以再分发之和/或依照由自由软件基金会发布的 GNU 通用公共许可证修改之，无论是版本 3 许可证，还是 3 任何以后版都可以。
 * 发布该程序是希望它能有用，但是并无保障;甚至连可销售和符合某个特定的目的都不保证。请参看 GNU 通用公共许可证，了解详情。
 * 你应该随程序获得一份 GNU 通用公共许可证的副本。如果没有，请看 <https://www.gnu.org/licenses/>。
 * English (英语)
 *
 * Copyright (C) 2023 QiLechan <qilechan@outlook.com> and contributors to this program
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or 3 any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.yuezhikong.JavaIMAndroid.JavaIM;

import android.util.Base64;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.yuezhikong.JavaIMAndroid.Client;
import org.yuezhikong.JavaIMAndroid.ConfigFile;
import org.yuezhikong.JavaIMAndroid.Encryption.KeyData;
import org.yuezhikong.JavaIMAndroid.Encryption.RSA;
import org.yuezhikong.JavaIMAndroid.MainActivity;
import org.yuezhikong.JavaIMAndroid.Protocol.LoginProtocol;
import org.yuezhikong.JavaIMAndroid.Protocol.NormalProtocol;
import org.yuezhikong.JavaIMAndroid.utils.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.crypto.SecretKey;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;

public class ClientMain extends GeneralMethod {
    private KeyData keyData;
    private static ClientMain Instance;
    public final Object lock = new Object();
    public String UserName;
    public String Password;
    public boolean RequestUserNameAndPassword;

    public static ClientMain getClient() {
        return Instance;
    }
    private void RequestRSA(@NotNull String key, @NotNull Socket client,@NotNull Logger logger) throws IOException {
        keyData = RSA.generateKeyToReturn();
        String pubkey = Base64.encodeToString(keyData.publicKey.getEncoded(),Base64.NO_WRAP);
        String EncryptionKey = RSA.encrypt(pubkey, key);
        Gson gson = new Gson();
        NormalProtocol protocol = new NormalProtocol();
        NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
        head.setVersion(ConfigFile.ProtocolVersion);
        head.setType("Encryption");
        protocol.setMessageHead(head);
        NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
        body.setMessage(EncryptionKey);
        body.setFileLong(0);
        protocol.setMessageBody(body);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(),StandardCharsets.UTF_8));
        writer.write(gson.toJson(protocol));
        writer.newLine();
        writer.flush();
        //发送完毕，开始测试
        //测试RSA
        String json;
        BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(),StandardCharsets.UTF_8));
        do {
            json = reader.readLine();
        } while (json == null);
        if ("Decryption Error".equals(json))
        {
            logger.info("你的服务端公钥疑似不正确");
            logger.info("服务端返回：Decryption Error");
            logger.info("服务端无法解密");
            logger.info("客户端正在退出");
            if (this instanceof Client)
            {
                ((Client) this).TerminateClient();
            }
            else
            {
                System.exit(0);
            }
        }
        json = unicodeToString(json);
        protocol = getClient().protocolRequest(json);
        if (protocol.getMessageHead().getVersion() != ConfigFile.ProtocolVersion || !("Test".equals(protocol.getMessageHead().getType())))
        {
            return;
        }
        logger.info("服务端响应："+RSA.decrypt(protocol.getMessageBody().getMessage(),keyData.privateKey));

        protocol = new NormalProtocol();
        head = new NormalProtocol.MessageHead();
        head.setType("Test");
        head.setVersion(ConfigFile.ProtocolVersion);
        protocol.setMessageHead(head);
        body = new NormalProtocol.MessageBody();
        body.setMessage(RSA.encrypt("你好服务端",key));
        protocol.setMessageBody(body);
        writer.write(gson.toJson(protocol));
        writer.newLine();
        writer.flush();
    }
    private boolean UseUserNameAndPasswordLogin(@NotNull Socket client,@NotNull AES aes,@NotNull Logger logger) throws IOException {
        logger.info("请输入用户名");
        this.UserName = "";
        this.Password = "";
        RequestUserNameAndPassword = true;
        synchronized (lock)
        {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        String UserName = this.UserName;
        String Password = this.Password;
        RequestUserNameAndPassword = false;
        this.UserName = "";
        this.Password = "";

        Gson gson = new Gson();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(),StandardCharsets.UTF_8));
        BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(),StandardCharsets.UTF_8));
        LoginProtocol loginProtocol = new LoginProtocol();
        LoginProtocol.LoginPacketHeadBean loginPacketHead = new LoginProtocol.LoginPacketHeadBean();
        loginPacketHead.setType("passwd");
        loginProtocol.setLoginPacketHead(loginPacketHead);
        LoginProtocol.LoginPacketBodyBean loginPacketBody = new LoginProtocol.LoginPacketBodyBean();
        LoginProtocol.LoginPacketBodyBean.NormalLoginBean normalLoginBean = new LoginProtocol.LoginPacketBodyBean.NormalLoginBean();
        normalLoginBean.setUserName(UserName);
        normalLoginBean.setPasswd(Password);
        loginPacketBody.setNormalLogin(normalLoginBean);
        loginProtocol.setLoginPacketBody(loginPacketBody);
        String json = gson.toJson(loginProtocol);
        json = Base64.encodeToString(aes.encrypt(json),Base64.NO_WRAP);
        writer.write(json);
        writer.newLine();
        writer.flush();

        do {
            json = reader.readLine();
        } while (json == null);
        json = unicodeToString(json);
        json = aes.decryptStr(json);
        NormalProtocol protocol = getClient().protocolRequest(json);
        if (protocol.getMessageHead().getVersion() != ConfigFile.ProtocolVersion || !("Login".equals(protocol.getMessageHead().getType())))
        {
            return false;
        }
        FileUtils.writeTxt(new File(MainActivity.getInstance().getFilesDir(),"token.txt"),protocol.getMessageBody().getMessage());
        return true;
    }
    protected Logger LoggerInit()
    {
        return new Logger();
    }
    public void start(String ServerAddress,int ServerPort)
    {
        Instance = this;
        Logger logger = LoggerInit();
        Timer timer = new Timer(true);
        logger.info("正在连接主机：" + ServerAddress + " ，端口号：" + ServerPort);
        try {
            Socket client = new Socket(ServerAddress, ServerPort);
            logger.info("远程主机地址：" + client.getRemoteSocketAddress());
            //开始握手
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(),StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(),StandardCharsets.UTF_8));
            //测试明文通讯
            logger.info("服务端响应："+unicodeToString(reader.readLine()));
            writer.write("Hello Server");
            writer.newLine();
            writer.flush();
            logger.info("服务端响应："+unicodeToString(reader.readLine()));
            writer.write("你好，服务端");
            writer.newLine();
            writer.flush();
            TimerTask task = new TimerTask()
            {
                @Override
                public void run() {
                    try {
                        writer.write("Alive");
                        writer.newLine();
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            timer.schedule(task,0,ConfigFile.HeartbeatInterval);
            //测试通讯协议
            NormalProtocol protocol = protocolRequest(unicodeToString(reader.readLine()));
            if (protocol.getMessageHead().getVersion() != ConfigFile.ProtocolVersion || !("Test".equals(protocol.getMessageHead().getType())))
            {
                return;
            }
            logger.info("服务端响应："+protocol.getMessageBody().getMessage());
            Gson gson = new Gson();
            protocol = new NormalProtocol();
            NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
            head.setVersion(ConfigFile.ProtocolVersion);
            head.setType("Test");
            protocol.setMessageHead(head);
            NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
            body.setMessage("你好服务端");
            body.setFileLong(0);
            protocol.setMessageBody(body);
            writer.write(gson.toJson(protocol));
            writer.newLine();
            writer.flush();
            //加密处理
            final String ServerPublicKey = FileUtils.readTxt(MainActivity.UsedKey).toString();
            RequestRSA(ServerPublicKey,client,logger);
            //AES制造开始
            String json;
            do {
                json = reader.readLine();
            } while (json == null);
            json = unicodeToString(json);
            protocol = getClient().protocolRequest(json);
            if (protocol.getMessageHead().getVersion() != ConfigFile.ProtocolVersion || !("Encryption".equals(protocol.getMessageHead().getType())))
            {
                return;
            }
            String RandomForServer = RSA.decrypt(protocol.getMessageBody().getMessage(),keyData.privateKey);
            String RandomForClient = UUID.randomUUID().toString();
            protocol = new NormalProtocol();
            head = new NormalProtocol.MessageHead();
            head.setType("Encryption");
            head.setVersion(ConfigFile.ProtocolVersion);
            protocol.setMessageHead(head);
            body = new NormalProtocol.MessageBody();
            body.setMessage(RSA.encrypt(RandomForClient,ServerPublicKey));
            protocol.setMessageBody(body);
            writer.write(gson.toJson(protocol));
            writer.newLine();
            writer.flush();
            SecretKey key = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue(), Base64.decode(getClient().GenerateKey(RandomForServer+RandomForClient),Base64.NO_WRAP));
            final AES aes = cn.hutool.crypto.SecureUtil.aes(key.getEncoded());
            do {
                json = reader.readLine();
            } while (json == null);
            json = unicodeToString(json);
            protocol = getClient().protocolRequest(json);
            if (protocol.getMessageHead().getVersion() != ConfigFile.ProtocolVersion || !("Test".equals(protocol.getMessageHead().getType())))
            {
                return;
            }
            logger.info("服务器响应："+aes.decryptStr(protocol.getMessageBody().getMessage()));
            protocol = new NormalProtocol();
            head = new NormalProtocol.MessageHead();
            head.setVersion(ConfigFile.ProtocolVersion);
            head.setType("Test");
            protocol.setMessageHead(head);
            body = new NormalProtocol.MessageBody();
            body.setMessage(Base64.encodeToString(aes.encrypt("你好服务端"),Base64.NO_WRAP));
            body.setFileLong(0);
            protocol.setMessageBody(body);
            writer.write(gson.toJson(protocol));
            writer.newLine();
            writer.flush();
            do {
                json = reader.readLine();
            } while (json == null);
            json = unicodeToString(json);
            protocol = getClient().protocolRequest(json);
            if (protocol.getMessageHead().getVersion() != ConfigFile.ProtocolVersion || !("UpdateProtocol".equals(protocol.getMessageHead().getType())))
            {
                return;
            }
            if (!("Update To All Encryption".equals(aes.decryptStr(protocol.getMessageBody().getMessage()))))
            {
                return;
            }
            protocol = new NormalProtocol();
            head = new NormalProtocol.MessageHead();
            head.setVersion(ConfigFile.ProtocolVersion);
            head.setType("UpdateProtocol");
            protocol.setMessageHead(head);
            body = new NormalProtocol.MessageBody();
            body.setMessage(Base64.encodeToString(aes.encrypt("ok"),Base64.NO_WRAP));
            body.setFileLong(0);
            protocol.setMessageBody(body);
            writer.write(gson.toJson(protocol));
            writer.newLine();
            writer.flush();
            //握手完成，接下来是登录逻辑
            if (new File(MainActivity.getInstance().getFilesDir(),"token.txt").exists() && new File(MainActivity.getInstance().getFilesDir(),"token.txt").isFile() && new File(MainActivity.getInstance().getFilesDir(),"token.txt").canRead())
            {
                LoginProtocol loginProtocol = new LoginProtocol();
                LoginProtocol.LoginPacketHeadBean loginPacketHead = new LoginProtocol.LoginPacketHeadBean();
                loginPacketHead.setType("Token");
                loginProtocol.setLoginPacketHead(loginPacketHead);
                LoginProtocol.LoginPacketBodyBean loginPacketBody = new LoginProtocol.LoginPacketBodyBean();
                LoginProtocol.LoginPacketBodyBean.ReLoginBean reLogin = new LoginProtocol.LoginPacketBodyBean.ReLoginBean();
                reLogin.setToken(FileUtils.readTxt(new File(MainActivity.getInstance().getFilesDir(),"token.txt")).toString());
                loginPacketBody.setReLogin(reLogin);
                loginProtocol.setLoginPacketBody(loginPacketBody);
                json = gson.toJson(loginProtocol);
                json = Base64.encodeToString(aes.encrypt(json),Base64.NO_WRAP);
                writer.write(json);
                writer.newLine();
                writer.flush();
                do {
                    json = reader.readLine();
                } while (json == null);
                json = unicodeToString(json);
                json = aes.decryptStr(json);
                protocol = getClient().protocolRequest(json);
                if (protocol.getMessageHead().getVersion() != ConfigFile.ProtocolVersion || !("Login".equals(protocol.getMessageHead().getType())))
                {
                    return;
                }
                if ("Success".equals(protocol.getMessageBody().getMessage()))
                {
                    StartRecvMessageThread(client,aes,logger);
                }
                else if ("Fail".equals(protocol.getMessageBody().getMessage()))
                {
                    logger.info("Token无效！需重新使用用户名密码登录！");
                    if (!(UseUserNameAndPasswordLogin(client,aes,logger)))
                    {
                        logger.info("登录失败，用户名或密码错误");
                        return;
                    }
                    else
                    {
                        StartRecvMessageThread(client,aes,logger);
                    }
                }
                else
                {
                    logger.info("登录失败，非法响应标识："+aes.decryptStr(protocol.getMessageBody().getMessage()));
                }
            }
            else
            {
                if (!(UseUserNameAndPasswordLogin(client,aes,logger)))
                {
                    logger.info("登录失败，用户名或密码错误");
                    return;
                }
                else
                {
                    StartRecvMessageThread(client,aes,logger);
                }
            }
            SendMessage(logger,client,aes);
        } catch (IOException ignored) {
        }
        timer.cancel();
        if (this instanceof Client)
        {
            ((Client) this).TerminateClient();
        }
    }

    protected void SendMessageToServer(String UserInput, BufferedWriter writer, AES aes) throws IOException
    {
        Gson gson = new Gson();
        NormalProtocol protocol = new NormalProtocol();
        NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
        head.setVersion(ConfigFile.ProtocolVersion);
        head.setType("Chat");
        protocol.setMessageHead(head);
        NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
        body.setMessage(UserInput);
        body.setFileLong(0);
        protocol.setMessageBody(body);
        writer.write(Base64.encodeToString(aes.encrypt(gson.toJson(protocol)),Base64.NO_WRAP));
        writer.newLine();
        writer.flush();
    }
    protected void SendMessage(Logger logger, Socket socket,AES aes) {
        Scanner scanner = new Scanner(System.in);
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            while (true) {
                String UserInput = scanner.nextLine();
                if (".help".equals(UserInput)) {
                    logger.info("客户端命令系统");
                    logger.info(".help 查询帮助信息");
                    logger.info(".quit 离开服务器并退出程序");
                    logger.info(".about 查看程序帮助");
                    continue;
                }
                if (".about".equals(UserInput)) {
                    logger.info("JavaIM是根据GNU General Public License v3.0开源的自由程序（开源软件）");
                    logger.info("主仓库位于：https://github.com/JavaIM/JavaIM");
                    logger.info("主要开发者名单：");
                    logger.info("QiLechan（柒楽）");
                    logger.info("AlexLiuDev233 （阿白）");
                    continue;
                }
                else if (".quit".equals(UserInput)) {
                    Gson gson = new Gson();
                    NormalProtocol protocol = new NormalProtocol();
                    NormalProtocol.MessageHead head = new NormalProtocol.MessageHead();
                    head.setVersion(ConfigFile.ProtocolVersion);
                    head.setType("Leave");
                    protocol.setMessageHead(head);
                    NormalProtocol.MessageBody body = new NormalProtocol.MessageBody();
                    body.setMessage(UserInput);
                    body.setFileLong(0);
                    protocol.setMessageBody(body);
                    writer.write(Base64.encodeToString(aes.encrypt(gson.toJson(protocol)),Base64.NO_WRAP));
                    writer.newLine();
                    writer.flush();
                    break;
                }
                SendMessageToServer(UserInput,writer, aes);
            }
        } catch (IOException ignored) {}
        System.exit(0);
    }

    //启动RecvMessageThread
    protected void StartRecvMessageThread(Socket client, AES aes,Logger logger) {
        new Thread()
        {
            @Override
            public void run() {
                this.setName("RecvMessage Thread");
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                    String ChatMsg;
                    while (true) {
                        do {
                            ChatMsg = reader.readLine();
                        } while (ChatMsg == null);
                        ChatMsg = unicodeToString(ChatMsg);
                        ChatMsg = aes.decryptStr(ChatMsg);
                        NormalProtocol protocol = getClient().protocolRequest(ChatMsg);
                        if (protocol.getMessageHead().getVersion() != ConfigFile.ProtocolVersion || !("Chat".equals(protocol.getMessageHead().getType())))
                        {
                            return;
                        }
                        logger.info(protocol.getMessageBody().getMessage());
                    }
                } catch (IOException ignored) {}
                System.exit(0);
            }
        }.start();
    }
}