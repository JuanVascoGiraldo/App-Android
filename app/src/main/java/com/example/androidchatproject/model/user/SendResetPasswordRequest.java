package com.example.androidchatproject.model.user;

import com.google.gson.annotations.SerializedName;

/**
 * Request para solicitar reset de contraseña
 */
public class SendResetPasswordRequest {
    
    @SerializedName("email")
    private String email;
    
    public SendResetPasswordRequest(String email) {
        this.email = email;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
}
