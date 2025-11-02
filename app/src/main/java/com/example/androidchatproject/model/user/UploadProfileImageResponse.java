package com.example.androidchatproject.model.user;

/**
 * Response para upload de imagen de perfil
 * POST /api/users/upload/profile/image
 */
public class UploadProfileImageResponse {
    
    private boolean valid;
    
    public UploadProfileImageResponse() {
    }
    
    public UploadProfileImageResponse(boolean valid) {
        this.valid = valid;
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
    }
}
