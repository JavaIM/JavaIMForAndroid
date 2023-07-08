package org.yuezhikong.JavaIMAndroid;

import com.google.gson.Gson;

import org.yuezhikong.JavaIMAndroid.JavaIM.ClientMain;
import org.yuezhikong.JavaIMAndroid.JavaIM.Logger;
import org.yuezhikong.JavaIMAndroid.Protocol.NormalProtocol;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import cn.hutool.crypto.symmetric.AES;

public class Client extends ClientMain {
    private Socket socket;
    private AES aes;
    private Thread RecvMessageThread;
    @Override
    protected void SendMessage(Logger logger, Socket socket, AES aes) {
        this.socket = socket;
        this.aes = aes;
    }

    public void MessageSendToServer(String Message) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
                    SendMessageToServer(Message,writer, aes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
    @Override
    protected void StartRecvMessageThread(Socket client, AES aes, Logger logger) {
        RecvMessageThread = new Thread()
        {
            public Thread start2() {
                start();
                return this;
            }

            @Override
            public void run() {
                this.setName("RecvMessage Thread");
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))) {
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
            }

        }.start2();
    }

    public void TerminateClient() {
        MainActivity.Session = false;
        new Thread()
        {

            @Override
            public void run() {
                this.setName("IO Worker");
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
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
                    writer.write(aes.encryptBase64(gson.toJson(protocol)));
                    writer.newLine();
                    writer.flush();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }.start();
        RecvMessageThread.interrupt();
    }
}
