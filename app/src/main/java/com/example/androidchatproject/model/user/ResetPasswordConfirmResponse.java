package com.example.androidchatproject.model.user;

import com.google.gson.annotations.SerializedName;

/**
 * Response al confirmar reset de contraseña
 */
public class ResetPasswordConfirmResponse {
    
    @SerializedName("success")
    private boolean success;
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
}
