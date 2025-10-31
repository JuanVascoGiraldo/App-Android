package com.example.androidchatproject.model.user;

public class SessionValidationRequest {
    private String token;

    public SessionValidationRequest(String token) {
        this.token = token;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
