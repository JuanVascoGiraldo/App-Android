package com.example.androidchatproject.model.chats;

import com.google.gson.annotations.SerializedName;

/**
 * Response al enviar un mensaje
 */
public class SendMessageResponse {
    
    @SerializedName("success")
    private boolean success;
    
    // Constructor vacío
    public SendMessageResponse() {
    }
    
    // Constructor completo
    public SendMessageResponse(boolean success) {
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
        return "SendMessageResponse{" +
                "success=" + success +
                '}';
    }
}
