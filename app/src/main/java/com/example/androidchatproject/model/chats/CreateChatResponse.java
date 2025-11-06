package com.example.androidchatproject.model.chats;

import com.google.gson.annotations.SerializedName;

/**
 * Response al crear un nuevo chat
 */
public class CreateChatResponse {
    
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("chat_id")
    private String chatId;
    
    // Constructor vacío
    public CreateChatResponse() {
    }
    
    // Constructor completo
    public CreateChatResponse(boolean success, String chatId) {
        this.success = success;
        this.chatId = chatId;
    }
    
    // Getters y Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getChatId() {
        return chatId;
    }
    
    public void setChatId(String chatId) {
        this.chatId = chatId;
    }
    
    @Override
    public String toString() {
        return "CreateChatResponse{" +
                "success=" + success +
                ", chatId='" + chatId + '\'' +
                '}';
    }
}
