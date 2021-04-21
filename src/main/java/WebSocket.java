import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.annotation.JSONField;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @ServerEndpoint 注解是一个类层次的注解，它的功能主要是将目前的类定义成一个websocket服务器端,
 * 注解的值将被用于监听用户连接的终端访问URL地址,客户端可以通过这个URL来连接到WebSocket服务器端
 */
@ServerEndpoint(value = "/websocket")
public class WebSocket {
    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static int onlineCount = 0;

    private String username = null;
    private String ID = null;

    //concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。若要实现服务端与单一客户端通信的话，可以使用Map来存放，其中Key可以为用户标识
    private static HashMap<String, WebSocket> webSocketSet = new HashMap<>();

    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;

    /**
     * 连接建立成功调用的方法
     *
     * @param session 可选的参数。session为与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    @OnOpen//注入
    public void onOpen(Session session) {
        this.session = session;
        this.ID = session.getId();
        webSocketSet.put(ID, this);//加入set中
        addOnlineCount();           //在线数加1
        System.out.println("有新连接加入！当前在线人数为" + getOnlineCount());
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        webSocketSet.remove(ID);  //从set中删除
        subOnlineCount();           //在线数减1
        sendGroupMessage(new Message(Dictionary.DELECTUSER, this.username + " " + this.session.getId(), this.ID, "GroupChat"));
        System.out.println("有一连接关闭！当前在线人数为" + getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param text    客户端发送过来的Json消息
     * @param session 可选的参数
     */
    @OnMessage
    public void onMessage(String text, Session session) {
        System.out.println("来自客户端的消息:" + (session.getId() + " " + getNickname() + "： " + text));
        Message message = JSON.parseObject(text, Message.class);
        switch (this.getType(message.getType())) {
            case NICKNAME:
                this.setNickname(message.getMessageContent());
                updateMyList();
                sendGroupMessage(new Message(Dictionary.ADDUSER, this.getNickname() + " " + this.ID, this.ID, "GroupChat"));
                try {
                    sendMessage(new Message(Dictionary.ID, this.ID, this.ID, this.ID));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case GROUPCHAT:
                message.setSenderID(session.getId());
                this.sendGroupMessage(message);
                try {
                    this.sendMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case PRIVATECHAT:
                this.sendIndividualMessage(message);
                break;
            case ADDUSER:
            case DELECTUSER:
            case ID:
                onError(null, null);
                break;
        }
    }

    /**
     * 发生错误时调用
     *
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("发生错误");
        error.printStackTrace();
    }

    public void setNickname(String name) {
        if (username == null) {
            username = name;
        }
    }

    public void updateMyList() {
        for (Map.Entry<String, WebSocket> entry : webSocketSet.entrySet()) {
            WebSocket item = entry.getValue();
            if (item.ID != this.ID && item.username != null) {
                try {
                    this.sendMessage(new Message(Dictionary.ADDUSER, item.getNickname() + " " + item.ID, this.ID, this.ID));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getNickname() {
        return username == null || username.equals("") ? "错误，用户名尚未设置" : username;
    }

    /**
     * 这个方法与上面几个方法不一样。没有用注解，是根据自己需要添加的方法。
     *
     * @param message
     * @throws IOException
     */
    public void sendMessage(Message message) throws IOException {
        this.session.getBasicRemote().sendText(JSONObject.toJSONString(message));
        //this.session.getAsyncRemote().sendText(message);
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebSocket.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WebSocket.onlineCount--;
    }

    public void sendGroupMessage(Message message) {
        for (Map.Entry<String, WebSocket> entry : webSocketSet.entrySet()) {
            WebSocket item = entry.getValue();
            if (item.ID == this.ID) {
                continue;
            }
            try {
                item.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendIndividualMessage(Message message) {
        String senderID = message.getSenderID();
        String receiverID = message.getReceiverID();
        WebSocket sender = webSocketSet.get(senderID);
        WebSocket receiver = webSocketSet.get(receiverID);
        try {
            receiver.sendMessage(message);
            sender.sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized JSONObject createCurrentUserInfo() {
        JSONObject userName = new JSONObject();
        for (Map.Entry<String, WebSocket> entry : webSocketSet.entrySet()) {
            WebSocket item = entry.getValue();
            if (Objects.equals(item.ID, this.ID)) {
                continue;
            }
            JSONObject userInfo = new JSONObject();
            userInfo.put("Username", item.getNickname());
            userInfo.put("UserID", item.ID);
            userName.put("UserInfo", userInfo);
        }
        return userName;
    }

    public JSONObject MessageWrapper(String text) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Message", text);
        return jsonObject;
    }

    public Dictionary getType(int index) {
        return Dictionary.class.getEnumConstants()[index];
    }

}

class Message {

    @JSONField(name = "Type")
    private int type;
    @JSONField(name = "MessageContent")
    private String messageContent;
    @JSONField(name = "SenderID")
    private String senderID;
    @JSONField(name = "ReceiverID")
    private String receiverID;

    public Message(int type, String messageContent, String senderID, String receiverID) {
        this.type = type;
        this.messageContent = messageContent;
        this.senderID = senderID;
        this.receiverID = receiverID;
    }

    public Message(Dictionary type, String messageContent, String senderID, String receiverID) {
        this.type = type.ordinal();
        this.messageContent = messageContent;
        this.senderID = senderID;
        this.receiverID = receiverID;
    }

    public Message() {
    }


    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public String getSenderID() {
        return senderID;
    }

    public void setSenderID(String senderID) {
        this.senderID = senderID;
    }

    public String getReceiverID() {
        return receiverID;
    }

    public void setReceiverID(String receiverID) {
        this.receiverID = receiverID;
    }

}

enum Dictionary {
    ADDUSER, DELECTUSER, NICKNAME, GROUPCHAT, PRIVATECHAT, ID
}