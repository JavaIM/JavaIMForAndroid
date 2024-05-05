package org.yuezhikong.Protocol;

public class ChatProtocol {

    /**
     * Message :
     * SourceUserName :
     */

    private String Message;
    private String SourceUserName;

    public String getMessage() {
        return Message;
    }

    public void setMessage(String Message) {
        this.Message = Message;
    }

    public String getSourceUserName() {
        return SourceUserName;
    }

    public void setSourceUserName(String SourceUserName) {
        this.SourceUserName = SourceUserName;
    }
}
