package com.example.androidchatproject.model.user;

public class AuthResponse {
    private String jwt;
    private String expirationDate;

    public String getJwt() { return jwt; }
    public String getExpirationDate() { return expirationDate; }
}
