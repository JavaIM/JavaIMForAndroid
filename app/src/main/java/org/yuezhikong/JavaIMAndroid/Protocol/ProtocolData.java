package org.yuezhikong.JavaIMAndroid.Protocol;

/**
 * 接受/发送的json的反序列化流程
 * 如果修改了protocol，请使用GsonFormat插件直接替换
 * 请勿在android替换，请去jdk版替换，更新时直接复制过来
 * jdk版本路径：org.yuezhikong.utils.ProtocolData
 */
public class ProtocolData {

    private MessageHead MessageHead;
    private MessageBody MessageBody;

    public MessageHead getMessageHead() {
        return MessageHead;
    }

    public void setMessageHead(MessageHead MessageHead) {
        this.MessageHead = MessageHead;
    }

    public MessageBody getMessageBody() {
        return MessageBody;
    }

    public void setMessageBody(MessageBody MessageBody) {
        this.MessageBody = MessageBody;
    }

    public static class MessageHead {
        /**
         * Version : 0
         * type :
         */

        private int Version;
        private String type;

        public int getVersion() {
            return Version;
        }

        public void setVersion(int Version) {
            this.Version = Version;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class MessageBody {
        /**
         * Message :
         * FileLong : 0
         */

        private String Message;
        private int FileLong;

        public String getMessage() {
            return Message;
        }

        public void setMessage(String Message) {
            this.Message = Message;
        }

        public int getFileLong() {
            return FileLong;
        }

        public void setFileLong(int FileLong) {
            this.FileLong = FileLong;
        }
    }
}