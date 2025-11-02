package com.example.androidchatproject.model.user;

/**
 * Response para verificación de email
 */
public class VerifyEmailResponse {
    
    private boolean valid;
    
    public VerifyEmailResponse() {
    }
    
    public VerifyEmailResponse(boolean valid) {
        this.valid = valid;
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
    }
}
