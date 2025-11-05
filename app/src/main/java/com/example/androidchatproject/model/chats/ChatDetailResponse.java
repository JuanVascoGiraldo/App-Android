package com.example.androidchatproject.model.chats;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Response detallado de un chat con sus mensajes
 */
public class ChatDetailResponse {
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("user")
    private String user;
    
    @SerializedName("username")
    private String username;
    
    @SerializedName("profile_img")
    private String profileImg;
    
    @SerializedName("messages")
    private List<MessageResponse> messages;
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("updated_at")
    private String updatedAt;
    
    // Constructor vacío
    public ChatDetailResponse() {
        this.messages = new ArrayList<>();
    }
    
    // Constructor completo
    public ChatDetailResponse(String id, String user, String username, String profileImg,
                             List<MessageResponse> messages, String createdAt, String updatedAt) {
        this.id = id;
        this.user = user;
        this.username = username;
        this.profileImg = profileImg;
        this.messages = messages != null ? messages : new ArrayList<>();
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
    
    public List<MessageResponse> getMessages() {
        return messages;
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
    
    public void setMessages(List<MessageResponse> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return "ChatDetailResponse{" +
                "id='" + id + '\'' +
                ", user='" + user + '\'' +
                ", username='" + username + '\'' +
                ", profileImg='" + profileImg + '\'' +
                ", messages=" + (messages != null ? messages.size() + " messages" : "null") +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                '}';
    }
}
