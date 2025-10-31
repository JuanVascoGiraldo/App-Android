package com.example.androidchatproject.model.user;


public class EmailVerificationResponse {
    private boolean valid;
    private String validUntil; // opcional, puede ser null

    public boolean isValid() { return valid; }
    public String getValidUntil() { return validUntil; }
}
