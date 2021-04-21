let TypeDictionary = {
    ADDUSER: 0, DELECTUSER: 1, NICKNAME: 2, GROUPCHAT: 3, PRIVATECHAT: 4, ID: 5
};

class selfInfo {
    static Nickname = null;
    static ID = null;
    static socket = null;
    static UserList = null;
    static currentState = null;
    static CurrentReceiverID = null;
}

let ChatLog = [];

function send() {
    let message = selfInfo.Nickname + "：" + document.getElementById("Text").value;
    let pack = packagingJSONString(selfInfo.currentState, message, selfInfo.ID,
        selfInfo.CurrentReceiverID === null ? "none" : selfInfo.CurrentReceiverID);
    console.log("from client: " + pack);
    selfInfo.socket.send(pack);
    document.getElementById("Text").value = "";
}

function addUserList(User) {
    let name = User.split(" ")[0];
    let id = User.split(' ')[1];
    selfInfo.UserList[id + ""] = name;

    let userList = document.getElementById("User");
    userList.innerHTML += "                <li>\n" +
        "                    <div class=\"SingleUser\" id = \"" + id + "\">\n" +
        "                        <div class=\"SingleUserIcon\"><img src=\"HeadIcon/" + Math.round(Math.random() * 9) + ".jpg\" alt=\"\"></div>\n" +
        "                        <diSv class=\"SingleUserName\">" + name + "</diSv>\n" +
        "                    </div>\n" +
        "                </li>"
    ChatLog[id + ""] = "";
}

function deleteUserList(User) {
    let name = User.split(" ")[0];
    let id = User.split(' ')[1];
    if (selfInfo.CurrentReceiverID === id) {
        click(id, "GroupChat");
    }
    delete selfInfo.UserList[id + ""];
    delete ChatLog[id + ""];
    document.getElementById(id).remove();
}


function setNickname() {
    let name = prompt("请输入用户名");
    while (name === null || name === "") {
        alert("请输入用户名");
        name = prompt("请输入用户名");
    }
    while (name.indexOf(" ") !== -1) {
        alert("名字不可有空格");
        name = prompt("请输入用户名");
    }
    alert("欢迎 " + name);
    selfInfo.Nickname = name;
}

function packagingJSONString(Type, MessageContent, SenderID, ReceiverID) {
    return ("{\"Type\":\"" + Type +
        "\",\"MessageContent\":\"" + MessageContent +
        "\",\"SenderID\":\"" + SenderID +
        "\",\"ReceiverID\":\"" + ReceiverID + "\"}");
}

function saveAndChange(fromID, toID) {
    ChatLog[fromID] = document.getElementById("OnChat").innerHTML;
    document.getElementById("OnChat").innerHTML = ChatLog[toID];
}

function updateState(toID) {
    if (toID === "GroupChat") {
        selfInfo.currentState = TypeDictionary.GROUPCHAT;
        selfInfo.CurrentReceiverID = "GroupChat";
        document.getElementById("Name").innerHTML = "群聊";
    } else {
        selfInfo.currentState = TypeDictionary.PRIVATECHAT;
        selfInfo.CurrentReceiverID = toID;
        document.getElementById("Name").innerHTML = selfInfo.UserList[toID];
    }
}

function click(fromID, toID) {
    $("#" + toID).removeClass("Highlight");
    saveAndChange(fromID, toID);
    updateState(toID);
}

selfInfo.CurrentReceiverID = "GroupChat";
selfInfo.UserList = [];
selfInfo.currentState = TypeDictionary.GROUPCHAT;
selfInfo.socket = new WebSocket("ws://localhost:8081/ChatRoom_war_exploded/websocket");

selfInfo.socket.onopen = function () {
    setNickname();
    console.log("from client: " + packagingJSONString(TypeDictionary.NICKNAME, selfInfo.Nickname, selfInfo.ID, "none"));
    selfInfo.socket.send(packagingJSONString(TypeDictionary.NICKNAME, selfInfo.Nickname, selfInfo.ID, "none"));
}
selfInfo.socket.onmessage = function (text) {
    console.log("from server: " + text.data);
    let message = JSON.parse(text.data);
    console.log(message.MessageContent);
    let messageText = message.MessageContent;
    switch (message.Type) {
        case TypeDictionary.NICKNAME:
            break;
        case TypeDictionary.PRIVATECHAT:
            if (selfInfo.currentState === TypeDictionary.GROUPCHAT ||
                !(selfInfo.CurrentReceiverID === message.SenderID || selfInfo.ID === message.SenderID)) {
                //todo:highlight,change info in chatlog
                $("#" + message.SenderID).addClass("Highlight");
                ChatLog[message.SenderID] += message.MessageContent + "<br>";
            } else {
                document.getElementById("OnChat").innerHTML += messageText + "<br>";
            }
            break;
        case TypeDictionary.GROUPCHAT:
            if (selfInfo.currentState === TypeDictionary.GROUPCHAT) {
                document.getElementById("OnChat").innerHTML += messageText + "<br>";
            } else {
                //todo:highlight,change info in chatlog
                $("#GroupChatRoom").addClass("Highlight");
                ChatLog["GroupChat"] += message.MessageContent + "<br>";
            }
            break;
        case TypeDictionary.ADDUSER:
            addUserList(message.MessageContent);
            let id = message.MessageContent.split(" ")[1];
            //$("#" +id).on("click",function () {click(selfInfo.CurrentReceiverID,id+"");});
            $(document).ready(function () {
                $(document).on("click", "#" + id, function () {
                    click(selfInfo.CurrentReceiverID, id + "");
                })
            });
            break;
        case TypeDictionary.DELECTUSER:
            deleteUserList(message.MessageContent);
            break;
        case TypeDictionary.ID:
            selfInfo.ID = message.MessageContent;
            break;
    }
}

$(document).ready(function () {
    $(document).on("click", "#GroupChatRoom", function () {
        $("#GroupChatRoom").removeClass("Highlight");
        click(selfInfo.CurrentReceiverID, "GroupChat");
    });
});










