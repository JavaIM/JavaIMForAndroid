package org.yuezhikong.JavaIMAndroid;

import static org.yuezhikong.JavaIMAndroid.ConfigFile.ProtocolVersion;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.view.View;
import android.widget.Button;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import org.yuezhikong.JavaIMAndroid.Encryption.KeyData;
import org.yuezhikong.JavaIMAndroid.Encryption.RSA;
import org.yuezhikong.JavaIMAndroid.Protocol.ProtocolData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity {
    private cn.hutool.crypto.symmetric.AES AES;
    private KeyData RSAKey;
    private boolean Session = false;
    private Socket socket;
    public static String ServerAddr = "";
    public static int ServerPort = 0;

    private void ErrorOutputToUserScreen(int id)
    {
        runOnUiThread(()->{
            TextView ErrorOutput = findViewById(R.id.Error);
            ErrorOutput.setText(id);
        });
    }

    public void ClearScreen(View view) {
        runOnUiThread(()->{
            TextView SocketDisplay = findViewById(R.id.ChatLog);
            SocketDisplay.setText("");
        });
    }

    class GoToSettingActivityListen implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            ChangeToSettingActivity(v);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView ChatLog = findViewById(R.id.ChatLog);
        ChatLog.setMovementMethod(ScrollingMovementMethod.getInstance());
        Button GoToSettingActivityButton = findViewById(R.id.button4);
        GoToSettingActivityButton.setOnClickListener(new GoToSettingActivityListen());
        requestPermission();
    }
    @SuppressLint("SetTextI18n")
    public void Connect(View view) {
        ((TextView)findViewById(R.id.Error)).setText("");
        if (socket == null)
        {
            Session = false;
        }
        else {
            if (socket.isClosed()) {
                Session = false;
            }
        }
        String IPAddress = ServerAddr;
        if (IPAddress.isEmpty())
        {
            TextView ErrorOutput = findViewById(R.id.Error);
            ErrorOutput.setText(R.string.Error1);
        }
        int port = ServerPort;
        if (port <= 0)
        {
            TextView ErrorOutput = findViewById(R.id.Error);
            ErrorOutput.setText(R.string.Error2);
        }
        if (port > 65535)
        {
            TextView ErrorOutput = findViewById(R.id.Error);
            ErrorOutput.setText(R.string.Error2);
        }
        if (!Session)
        {
            ((TextView)findViewById(R.id.ChatLog)).setText("");
            Session = true;
            Runnable NetworkThread = () ->
            {
                try {
                    //获取文件
                    File Storage = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/JavaIMFiles");
                    if (!Storage.exists()) {
                        if (!Storage.mkdir())
                        {
                            ErrorOutputToUserScreen(R.string.ErrorAccessDenied);
                        }
                    }
                    File ServerPublicKeyFile = new File(Storage.getAbsolutePath() + "/ServerPublicKey.key");
                    if (!ServerPublicKeyFile.exists()) {
                        runOnUiThread(()->{
                            TextView ErrorOutput = findViewById(R.id.Error);
                            ErrorOutput.setText(R.string.Error5);
                        });
                        Session = false;
                        return;
                    }
                    RSAKey = RSA.generateKeyToReturn();
                    // 创建Socket对象 & 指定服务端的IP 及 端口号
                    socket = new Socket(IPAddress, port);
                    @SuppressLint("SetTextI18n")
                    Runnable recvmessage = () ->
                    {
                        while (true)
                        {
                            BufferedReader reader;//获取输入流
                            try {
                                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                String msg = reader.readLine();
                                if (msg == null)
                                {
                                    ErrorOutputToUserScreen(R.string.Disconnected);
                                    System.exit(0);
                                    break;
                                }
                                msg = AES.decryptStr(msg);
                                msg = java.net.URLDecoder.decode(msg, "UTF-8");
                                // 将信息从Protocol Json中取出
                                Gson gson = new Gson();
                                ProtocolData protocolData = gson.fromJson(msg,ProtocolData.class);
                                if (protocolData.getMessageHead().getVersion() != ProtocolVersion)
                                {
                                    runOnUiThread(()->{
                                        TextView SocketDisplay = findViewById(R.id.ChatLog);
                                        SocketDisplay.setText(SocketDisplay.getText().toString()+"\r\n目标服务器协议版本与您客户端不符，目标服务器协议版本为："+protocolData.getMessageHead().getVersion()+"此客户端协议版本为："+ProtocolVersion);
                                    });
                                    socket.close();
                                    break;
                                }
                                // type目前只实现了chat,FileTransfer延后
                                if (protocolData.getMessageHead().getType().equals("Chat"))
                                {
                                    msg = protocolData.getMessageBody().getMessage();
                                    String finalMsg = msg;
                                    runOnUiThread(()->{
                                        TextView SocketDisplay = findViewById(R.id.ChatLog);
                                        SocketDisplay.setText(SocketDisplay.getText().toString() + "\r\n" + finalMsg);
                                    });
                                }
                                else if (protocolData.getMessageHead().getType().equals("FileTransfer"))
                                {
                                    runOnUiThread(()->{
                                        TextView SocketDisplay = findViewById(R.id.ChatLog);
                                        SocketDisplay.setText(SocketDisplay.getText().toString()+"\r\n有人想要为您发送一个文件，但是此客户端暂不支持FileTransfer协议");
                                    });
                                }
                                else
                                {
                                    runOnUiThread(()->{
                                        TextView SocketDisplay = findViewById(R.id.ChatLog);
                                        SocketDisplay.setText(SocketDisplay.getText().toString()+"\r\n服务端发来无法识别的非法数据包");
                                    });
                                }
                            }
                            catch (IOException e)
                            {
                                if (!"Connection reset by peer".equals(e.getMessage()) && !"Connection reset".equals(e.getMessage()) && !"Socket is closed".equals(e.getMessage()))  {
                                    e.printStackTrace();
                                }
                                else
                                {
                                    ErrorOutputToUserScreen(R.string.Disconnected);
                                    System.exit(0);
                                    break;
                                }
                            }
                        }
                    };
                    TextView SocketOutput = findViewById(R.id.ChatLog);
                    runOnUiThread(()->{
                        SocketOutput.setText(SocketOutput.getText() + "\r\n连接到主机：" + IPAddress + " ，端口号：" + port);
                        SocketOutput.setText(SocketOutput.getText() + "\r\n" + socket.getRemoteSocketAddress().toString());
                    });
                    String ServerPublicKey = Objects.requireNonNull(RSA.loadPublicKeyFromFile(ServerPublicKeyFile.getAbsolutePath())).PublicKey;
                    OutputStream outToServer = socket.getOutputStream();
                    DataOutputStream out = new DataOutputStream(outToServer);
                    InputStream inFromServer = socket.getInputStream();
                    DataInputStream in = new DataInputStream(inFromServer);

                    String ClientRSAKey = java.net.URLEncoder.encode(Base64.encodeToString(RSAKey.publicKey.getEncoded(),Base64.NO_WRAP), "UTF-8");//通讯握手开始
                    out.writeUTF(ClientRSAKey);
                    String DecryptStr = RSA.decrypt(in.readUTF(),RSAKey.privateKey);
                    String finalDecryptStr1 = DecryptStr;
                    runOnUiThread(()-> SocketOutput.setText(SocketOutput.getText()+"\r\n服务器响应："+ finalDecryptStr1));
                    out.writeUTF(RSA.encrypt("Hello,Server! This Message By Client RSA System",ServerPublicKey));
                    String RandomByClient = UUID.randomUUID().toString();
                    out.writeUTF(RSA.encrypt(java.net.URLEncoder.encode(RandomByClient, "UTF-8"),ServerPublicKey));
                    String RandomByServer = java.net.URLDecoder.decode(RSA.decrypt(in.readUTF(),RSAKey.privateKey),"UTF-8");
                    byte[] KeyByte = new byte[32];
                    byte[] SrcByte = Base64.encodeToString((RandomByClient+RandomByServer).getBytes(StandardCharsets.UTF_8),Base64.NO_WRAP).getBytes(StandardCharsets.UTF_8);
                    System.arraycopy(SrcByte,0,KeyByte,0,31);
                    SecretKey key = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue(),KeyByte);
                    AES = cn.hutool.crypto.SecureUtil.aes(key.getEncoded());
                    DecryptStr = AES.decryptStr(in.readUTF());
                    String finalDecryptStr = DecryptStr;
                    runOnUiThread(()-> SocketOutput.setText(SocketOutput.getText() + "\r\n服务器响应："+ finalDecryptStr));
                    out.writeUTF(Base64.encodeToString(AES.encrypt("Hello,Server! This Message By Client AES System"),Base64.NO_WRAP));
                    out.writeUTF("Hello from " + socket.getLocalSocketAddress());
                    String Message = in.readUTF();
                    runOnUiThread(()-> SocketOutput.setText(SocketOutput.getText() + "\r\n服务器响应： " + Message));//握手结束
                    Thread thread = new Thread(recvmessage);
                    thread.start();
                    thread.setName("RecvMessage Thread");
                } catch (IOException e) {
                    runOnUiThread(()->{
                        TextView ErrorOutput = findViewById(R.id.Error);
                        ErrorOutput.setText(R.string.Error3);
                    });
                }
            };
            Thread NetworKThread = new Thread(NetworkThread);
            NetworKThread.start();
            NetworKThread.setName("Network Thread");
        }
    }
    private void requestPermission() {
        XXPermissions.with(this).permission(Permission.MANAGE_EXTERNAL_STORAGE)
                .request(new OnPermissionCallback() {

                             @Override
                             public void onGranted(@NonNull List<String> permissions, boolean all) {
                             }

                             @Override
                             public void onDenied(@NonNull List<String> permissions, boolean never) {
                        if (never) {
                            // 如果是被永久拒绝就跳转到应用权限系统设置页面
                            XXPermissions.startPermissionActivity(MainActivity.this, permissions);
                        }
                    }
                });
    }

    //用户按下发送按钮
    public void Send(View view) {
        ((TextView)findViewById(R.id.Error)).setText("");
        if (socket == null)
        {
            Session = false;
        }
        else {
            if (socket.isClosed()) {
                Session = false;
            }
        }
        EditText UserMessageText = findViewById (R.id.UserSendMessage);
        String UserMessage = UserMessageText.getText().toString();
        if (!Session)
        {
            TextView ErrorOutput = findViewById(R.id.Error);
            ErrorOutput.setText(R.string.Error6);
        }
        else
        {
            if (socket == null)
            {
                Session = false;
                return;
            }
            final String UserMessageFinal = UserMessage;
            @SuppressLint("SetTextI18n") Runnable NetworkThreadRunnable = ()->{
                BufferedWriter writer = null;
                try {
                    writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                boolean clientcommand = false;
                if (".about".equals(UserMessageFinal))
                {
                    TextView SocketOutput = findViewById(R.id.ChatLog);
                    runOnUiThread(()->{
                        SocketOutput.setText(SocketOutput.getText() + "JavaIM是根据GNU General Public License v3.0开源的自由程序（开源软件）\r\n");
                        SocketOutput.setText(SocketOutput.getText() + "主仓库位于：https://github.com/QiLechan/JavaIM\r\n");
                        SocketOutput.setText(SocketOutput.getText() + "主要开发者名单：\r\n");
                        SocketOutput.setText(SocketOutput.getText() + "QiLechan（柒楽）\r\n");
                        SocketOutput.setText(SocketOutput.getText() + "AlexLiuDev233 （阿白）\r\n");
                        SocketOutput.setText(SocketOutput.getText() + "仓库启用了不允许协作者直接推送到主分支，需审核后再提交\r\n");
                        SocketOutput.setText(SocketOutput.getText() + "因此，如果想要体验最新功能，请查看fork仓库，但不保证稳定性\r\n");
                    });
                    clientcommand = true;
                }
                if (!clientcommand) {
                    // 应当在这里加入json处理
                    String input = UserMessageFinal;
                    Gson gson = new Gson();
                    ProtocolData protocolData = new ProtocolData();
                    ProtocolData.MessageHead MessageHead = new ProtocolData.MessageHead();
                    MessageHead.setVersion(ProtocolVersion);
                    MessageHead.setType("Chat");
                    protocolData.setMessageHead(MessageHead);
                    ProtocolData.MessageBody MessageBody = new ProtocolData.MessageBody();
                    MessageBody.setFileLong(0);
                    MessageBody.setMessage(input);
                    protocolData.setMessageBody(MessageBody);
                    input = gson.toJson(protocolData);
                    // 加密信息
                    try {
                        input = java.net.URLEncoder.encode(input, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    input = Base64.encodeToString(AES.encrypt(input),Base64.NO_WRAP);
                    // 发送消息给服务器
                    try {
                        Objects.requireNonNull(writer).write(input);
                        writer.newLine();
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            Thread NetWorkThread = new Thread(NetworkThreadRunnable);
            NetWorkThread.start();
            NetWorkThread.setName("Network Thread");
        }
    }
    public void Disconnect(View view) {
        ((TextView)findViewById(R.id.Error)).setText("");
        if (socket == null)
        {
            Session = false;
        }
        else {
            if (socket.isClosed()) {
                Session = false;
            }
        }
        if (Session)
        {
            Session = false;
            Runnable NetworkThread = () ->
            {
                try {
                    BufferedWriter writer = null;
                    try {
                        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (writer == null)
                    {
                        socket.close();
                        return;
                    }
                    writer.write("quit\n");
                    writer.newLine();
                    writer.flush();
                    writer.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };
            Thread NetworKThread = new Thread(NetworkThread);
            NetworKThread.start();
            NetworKThread.setName("Network Thread");
        }
    }
    public void ChangeToSettingActivity(View view) {
        String tmpServerAddr;
        int tmpServerPort;
        //处理当前已经记录的Addr和Port
        if (ServerAddr == null)
        {
            tmpServerAddr = "";
        }
        else
        {
            tmpServerAddr = ServerAddr;
        }
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
        startActivity(intent);
    }

}