package com.example.androidchatproject.model.user;

/**
 * Response para reenvío de código de verificación
 */
public class ResendVerificationResponse {
    
    private boolean success;
    
    public ResendVerificationResponse() {
    }

    public ResendVerificationResponse(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
}
