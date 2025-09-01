package com.example.easychat.model;

import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatroomModel {
    String chatroomId;
    List<String> userIds;
    Timestamp lastMessageTimestamp;
    String lastMessageSenderId;
    String lastMessage;
    String groupName;
    boolean isGroupChat;
    Map<String, Boolean> customNotificationStatus;
    String pinnedMessageId;

    public ChatroomModel() {
        // Construtor padrão agora inicializa os campos para evitar valores nulos
        this.lastMessageTimestamp = Timestamp.now();
        this.lastMessage = "";
        this.customNotificationStatus = new HashMap<>();
    }

    public ChatroomModel(String chatroomId, List<String> userIds, Timestamp lastMessageTimestamp, String lastMessageSenderId, String groupName, boolean isGroupChat) {
        this.chatroomId = chatroomId;
        this.userIds = userIds;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.lastMessageSenderId = lastMessageSenderId;
        this.groupName = groupName;
        this.isGroupChat = isGroupChat;
        this.lastMessage = ""; // Garante que nunca seja nulo
        this.customNotificationStatus = new HashMap<>();
        this.pinnedMessageId = null;
    }

    // Getters e Setters
    public String getPinnedMessageId() {
        return pinnedMessageId;
    }
    public void setPinnedMessageId(String pinnedMessageId) {
        this.pinnedMessageId = pinnedMessageId;
    }
    public Map<String, Boolean> getCustomNotificationStatus() { return customNotificationStatus; }
    public void setCustomNotificationStatus(Map<String, Boolean> customNotificationStatus) { this.customNotificationStatus = customNotificationStatus; }
    public String getChatroomId() { return chatroomId; }
    public void setChatroomId(String chatroomId) { this.chatroomId = chatroomId; }
    public List<String> getUserIds() { return userIds; }
    public void setUserIds(List<String> userIds) { this.userIds = userIds; }
    public Timestamp getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(Timestamp lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }
    public String getLastMessageSenderId() { return lastMessageSenderId; }
    public void setLastMessageSenderId(String lastMessageSenderId) { this.lastMessageSenderId = lastMessageSenderId; }
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public boolean isGroupChat() { return isGroupChat; }
    public void setGroupChat(boolean groupChat) { isGroupChat = groupChat; }
}