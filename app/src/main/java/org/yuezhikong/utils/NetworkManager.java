package org.yuezhikong.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * 网络管理器
 * 使用此类更换底层网络层
 */
public final class NetworkManager {
    public final static class NetworkData implements Closeable
    {
        private final Socket socket;
        private final ServerSocket serverSocket;
        private BufferedReader reader;

        private Socket getSocket() {
            if (socket == null)
                throw new RuntimeException("The Network Data is Server Mode!");
            return socket;
        }

        private BufferedReader getReader()
        {
            if (socket == null)
                throw new RuntimeException("The Network Data is Server Mode!");
            return reader;
        }


        public ServerSocket getServerSocket() {
            if (serverSocket == null)
                throw new RuntimeException("The Network Data is Client Mode!");
            return serverSocket;
        }

        private NetworkData(@NotNull ServerSocket socket)
        {
            this.serverSocket = socket;
            this.socket = null;
        }

        private NetworkData(@NotNull Socket socket) throws IOException {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.serverSocket = null;
            this.socket = socket;
        }

        @Override
        public void close() throws IOException {
            if (socket != null)
                NetworkManager.ShutdownTCPConnection(this);
            else
                NetworkManager.ShutdownTCPServer(this);
        }

        public SocketAddress getRemoteSocketAddress()
        {
            if (socket != null)
                return socket.getRemoteSocketAddress();
            else
                throw new RuntimeException("Broken Network Data");
        }

    }

    private NetworkManager() {}

    /**
     * 写入数据到远程
     * @param RemoteNetworkData 远程网络数据
     * @param Data 数据
     * @throws IllegalArgumentException 网络数据无效或不兼容
     * @throws IOException 发生IO错误
     */
    public static void WriteDataToRemote(@NotNull NetworkData RemoteNetworkData,String Data) throws IOException {
        OutputStream SocketOutputStream;
        if (RemoteNetworkData.getSocket() != null)
            SocketOutputStream = RemoteNetworkData.getSocket().getOutputStream();
        else
            throw new IllegalArgumentException("The Remote Network Data is Invalid!");

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(SocketOutputStream, StandardCharsets.UTF_8));
        writer.write(Data);
        writer.newLine();
        writer.flush();
    }

    /**
     * 从远程获取数据
     * @param RemoteNetworkData 远程网络数据
     * @param NumberOfRetry 失败时重试次数
     * @return 远程返回的数据
     * @throws IllegalArgumentException 远程网络数据无效或NumberOfRetry不是-1或正数
     * @throws IOException 发生IO错误
     */

    public static @Nullable String RecvDataFromRemote(@NotNull NetworkData RemoteNetworkData, final int NumberOfRetry) throws IOException {
        if (NumberOfRetry < -1)
            throw new IllegalArgumentException("NumberOfRetry must be positive or -1");

        int NumberOfRetryFork = NumberOfRetry;

        BufferedReader reader;
        if (RemoteNetworkData.getReader() != null)
            reader = RemoteNetworkData.getReader();
        else
            throw new IllegalArgumentException("The Remote Network Data is Invalid!");

        String data;
        do {
            if (NumberOfRetryFork != -1)
            {
                if (NumberOfRetryFork == 0)
                    return null;
                NumberOfRetryFork-=1;
            }

            data = reader.readLine();
            if (data != null && data.equals("Alive"))
            {
                data = null;
            }

            if (RemoteNetworkData.getSocket().isClosed())
            {
                return null;
            }
        } while (data == null);
        return UnicodeToString.unicodeToString(data);
    }

    /**
     * 从远程获取数据
     * @param RemoteNetworkData 远程网络数据
     * @return 远程返回的数据
     * @throws IllegalArgumentException 远程网络数据无效或NumberOfRetry不是-1或正数
     * @throws IOException 发生IO错误
     */
    public static @Nullable String RecvDataFromRemote(@NotNull NetworkData RemoteNetworkData) throws IOException
    {
        return RecvDataFromRemote(RemoteNetworkData,-1);
    }

    /**
     * 连接到TCP服务器
     * @param ServerAddress 服务器地址
     * @param ServerPort 服务器端口
     * @return TCP连接数据
     * @throws IOException 发生IO错误
     * @throws SocketConnectTimeoutException socket连接超时
     */
    @Contract("_,_ -> new")
    public static @NotNull NetworkData ConnectToTCPServer(@NotNull String ServerAddress, int ServerPort) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(ServerAddress,ServerPort), 5000);
        if (!socket.isConnected())
        {
            throw new SocketConnectTimeoutException("The Server Address can not be connect!");
        }
        return new NetworkData(socket);
    }
    public static class SocketConnectTimeoutException extends RuntimeException
    {
        public SocketConnectTimeoutException(String msg)
        {
            super(msg);
        }
    }

    /**
     * 关闭TCP连接
     * @param TCPConnection TCP连接数据
     * @throws IOException 发生IO错误
     */
    public static void ShutdownTCPConnection(@NotNull NetworkData TCPConnection) throws IOException {
        if (TCPConnection.getSocket() == null)
            throw new IllegalArgumentException("The Network Data is Invalid!");
        if (TCPConnection.getSocket().isClosed())
            return;
        TCPConnection.getSocket().close();
    }

    /**
     * 关闭TCP服务器
     * @param TCPServerData 服务器网络数据
     * @throws IOException 发生IO错误
     */
    public static void ShutdownTCPServer(@NotNull NetworkData TCPServerData) throws IOException {
        if (TCPServerData.getServerSocket() == null)
            throw new IllegalArgumentException("The Network Data is Invalid!");
        if (TCPServerData.getServerSocket().isClosed())
            return;
        TCPServerData.getServerSocket().close();
    }

}
