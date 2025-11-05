package com.example.androidchatproject.model.chats;

import com.google.gson.annotations.SerializedName;

/**
 * Request para enviar un mensaje en un chat existente
 */
public class SendMessageRequest {
    
    @SerializedName("chat_id")
    private String chatId;
    
    @SerializedName("content")
    private String content;
    
    // Constructor vacío
    public SendMessageRequest() {
    }
    
    // Constructor con solo chat_id y content
    public SendMessageRequest(String chatId, String content) {
        this.chatId = chatId;
        this.content = content;
    }
    
    // Getters y Setters
    public String getChatId() {
        return chatId;
    }
    
    public void setChatId(String chatId) {
        this.chatId = chatId;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    @Override
    public String toString() {
        return "SendMessageRequest{" +
                "chatId='" + chatId + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
