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

    //用来存放每个客户端对应的MyWebSocket对象
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
     * 按照消息类型反应不同的动作
     * @param text    客户端发送过来的Json消息
     * @param session 可选的参数
     */
    @OnMessage
    public void onMessage(String text, Session session) {
        System.out.println("来自客户端的消息:" + (session.getId() + " " + getNickname() + "： " + text));
        Message message = JSON.parseObject(text, Message.class);//FastJSON 负责把json信息转化为对象
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
    /*
       todo：我想这里也是一个难点，图片的传输，表情就算了吧，毕竟emoji又不是不能用
       todo：servlet完成的一个图片传输，应在这个数据的交互这里了，给定一个链接，然后再传过去
       todo：或者使用websocket来执行，传输位文件
     */
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
//设置用户名
    public void setNickname(String name) {
        if (username == null) {
            username = name;
        }
    }
//更新当前客户端的用户列表
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
//获得当前用户名
    public String getNickname() {
        return username == null || username.equals("") ? "错误，用户名尚未设置" : username;
    }
    //做好封装之后的发送信息
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
//把除了自身之外的全部客户端都发消息
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
//私聊消息
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
//获得索引对应的类型信息
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