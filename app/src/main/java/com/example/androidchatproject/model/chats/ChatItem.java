package com.example.androidchatproject.model.chats;

import com.google.gson.annotations.SerializedName;

/**
 * Modelo para un chat individual en la lista
 */
public class ChatItem {
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("user")
    private String user;
    
    @SerializedName("username")
    private String username;
    
    @SerializedName("profile_img")
    private String profileImg;
    
    @SerializedName("last_message")
    private String lastMessage;
    
    @SerializedName("last_message_time")
    private String lastMessageTime;
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("updated_at")
    private String updatedAt;
    
    // Constructor vacío
    public ChatItem() {
    }
    
    // Constructor completo
    public ChatItem(String id, String user, String username, String profileImg, 
                    String lastMessage, String lastMessageTime, String createdAt, String updatedAt) {
        this.id = id;
        this.user = user;
        this.username = username;
        this.profileImg = profileImg;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public String getUser() {
        return user;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getProfileImg() {
        return profileImg;
    }
    
    public String getLastMessage() {
        return lastMessage;
    }
    
    public String getLastMessageTime() {
        return lastMessageTime;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public String getUpdatedAt() {
        return updatedAt;
    }
    
    // Setters
    public void setId(String id) {
        this.id = id;
    }
    
    public void setUser(String user) {
        this.user = user;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public void setProfileImg(String profileImg) {
        this.profileImg = profileImg;
    }
    
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
    
    public void setLastMessageTime(String lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return "ChatItem{" +
                "id='" + id + '\'' +
                ", user='" + user + '\'' +
                ", username='" + username + '\'' +
                ", profileImg='" + profileImg + '\'' +
                ", lastMessage='" + lastMessage + '\'' +
                ", lastMessageTime='" + lastMessageTime + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                '}';
    }
}
