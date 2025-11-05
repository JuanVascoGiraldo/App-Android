package com.example.androidchatproject.model.chats;

import com.google.gson.annotations.SerializedName;

/**
 * Response al crear un nuevo chat
 */
public class CreateChatResponse {
    
    @SerializedName("success")
    private boolean success;
    
    // Constructor vacío
    public CreateChatResponse() {
    }
    
    // Constructor completo
    public CreateChatResponse(boolean success) {
        this.success = success;
    }
    
    // Getter y Setter
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    @Override
    public String toString() {
        return "CreateChatResponse{" +
                "success=" + success +
                '}';
    }
}
