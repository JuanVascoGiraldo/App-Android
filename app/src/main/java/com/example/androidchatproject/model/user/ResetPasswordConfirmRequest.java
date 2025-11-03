package com.example.androidchatproject.model.user;

import com.google.gson.annotations.SerializedName;

/**
 * Request para confirmar reset de contraseña
 */
public class ResetPasswordConfirmRequest {
    
    @SerializedName("recover_id")
    private String recoverId;
    
    @SerializedName("recover_token")
    private String recoverToken;
    
    @SerializedName("recover_code")
    private String recoverCode;
    
    @SerializedName("password")
    private String password;
    
    @SerializedName("re_password")
    private String rePassword;
    
    public ResetPasswordConfirmRequest(String recoverId, String recoverToken, String recoverCode, 
                                       String password, String rePassword) {
        this.recoverId = recoverId;
        this.recoverToken = recoverToken;
        this.recoverCode = recoverCode;
        this.password = password;
        this.rePassword = rePassword;
    }
    
    // Getters
    public String getRecoverId() {
        return recoverId;
    }
    
    public String getRecoverToken() {
        return recoverToken;
    }
    
    public String getRecoverCode() {
        return recoverCode;
    }
    
    public String getPassword() {
        return password;
    }
    
    public String getRePassword() {
        return rePassword;
    }
    
    // Setters
    public void setRecoverId(String recoverId) {
        this.recoverId = recoverId;
    }
    
    public void setRecoverToken(String recoverToken) {
        this.recoverToken = recoverToken;
    }
    
    public void setRecoverCode(String recoverCode) {
        this.recoverCode = recoverCode;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public void setRePassword(String rePassword) {
        this.rePassword = rePassword;
    }
}
