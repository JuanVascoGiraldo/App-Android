package com.example.androidchatproject.model.user;

public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String re_password;

    public RegisterRequest(String username, String email, String password, String rePassword) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.re_password = rePassword;
    }

    // Getters
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRe_password() { return re_password; }
    
    // Setters
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setRe_password(String re_password) { this.re_password = re_password; }
}
