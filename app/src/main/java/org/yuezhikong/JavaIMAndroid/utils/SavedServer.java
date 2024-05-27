package org.yuezhikong.JavaIMAndroid.utils;

import java.util.List;

public class SavedServer {

    private List<Server> Servers;

    public List<Server> getServers() {
        return Servers;
    }

    public void setServers(List<Server> Servers) {
        this.Servers = Servers;
    }

    public static class Server {
        /**
         * ServerAddress :
         * ServerLoginToken :
         * ServerName :
         * ServerPort : 0
         * X509CertContent :
         * isUsingServer : false
         */

        private String ServerAddress;
        private String ServerLoginToken;
        private String ServerName;
        private int ServerPort;
        private String X509CertContent;
        private boolean isUsingServer;

        public String getServerAddress() {
            return ServerAddress;
        }

        public void setServerAddress(String ServerAddress) {
            this.ServerAddress = ServerAddress;
        }

        public String getServerLoginToken() {
            return ServerLoginToken;
        }

        public void setServerLoginToken(String ServerLoginToken) {
            this.ServerLoginToken = ServerLoginToken;
        }

        public String getServerName() {
            return ServerName;
        }

        public void setServerName(String ServerName) {
            this.ServerName = ServerName;
        }

        public int getServerPort() {
            return ServerPort;
        }

        public void setServerPort(int ServerPort) {
            this.ServerPort = ServerPort;
        }

        public String getX509CertContent() {
            return X509CertContent;
        }

        public void setX509CertContent(String X509CertContent) {
            this.X509CertContent = X509CertContent;
        }

        public boolean isIsUsingServer() {
            return isUsingServer;
        }

        public void setIsUsingServer(boolean isUsingServer) {
            this.isUsingServer = isUsingServer;
        }
    }
}
