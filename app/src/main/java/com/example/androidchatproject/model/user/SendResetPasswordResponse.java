package com.example.androidchatproject.model.user;

import com.google.gson.annotations.SerializedName;

/**
 * Response al solicitar reset de contraseña
 */
public class SendResetPasswordResponse {
    
    @SerializedName("valid_until")
    private String validUntil;
    
    @SerializedName("token")
    private String token;
    
    @SerializedName("id")
    private String id;
    
    // Getters
    public String getValidUntil() {
        return validUntil;
    }
    
    public String getToken() {
        return token;
    }
    
    public String getId() {
        return id;
    }
    
    // Setters
    public void setValidUntil(String validUntil) {
        this.validUntil = validUntil;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public void setId(String id) {
        this.id = id;
    }
}
