package com.example.androidchatproject.model.user;


public class TokenValidationResponse {
    private String validUntil;
    private boolean valid;

    public String getValidUntil() { return validUntil; }
    public boolean isValid() { return valid; }
}
