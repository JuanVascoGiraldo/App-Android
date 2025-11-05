package com.example.androidchatproject.model.chats;

import com.google.gson.annotations.SerializedName;

/**
 * Request para crear un nuevo chat
 */
public class CreateChatRequest {
    
    @SerializedName("user_id")
    private String userId;
    
    @SerializedName("content")
    private String content;
    
    // Constructor vacío
    public CreateChatRequest() {
    }
    
    // Constructor completo
    public CreateChatRequest(String userId, String content) {
        this.userId = userId;
        this.content = content;
    }
    
    // Getters y Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    @Override
    public String toString() {
        return "CreateChatRequest{" +
                "userId='" + userId + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
