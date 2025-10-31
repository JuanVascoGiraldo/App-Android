package com.example.androidchatproject.model.user;

import com.google.gson.annotations.SerializedName;

public class AuthRequest {
    private String email;
    private String password;
    
    @SerializedName("login_remember")
    private boolean loginRemember;

    public AuthRequest(String email, String password, boolean loginRemember) {
        this.email = email;
        this.password = password;
        this.loginRemember = loginRemember;
    }

    // Getters y Setters
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public boolean isLoginRemember() { return loginRemember; }
}
