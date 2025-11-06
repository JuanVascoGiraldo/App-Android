package com.example.androidchatproject.model.user;

import com.google.gson.annotations.SerializedName;

/**
 * Modelo para la respuesta del perfil de usuario
 * GET /api/
 */
public class UserProfileResponse {
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("username")
    private String username;
    
    @SerializedName("email_is_verified")
    private boolean emailIsVerified;
    
    @SerializedName("profile_image_url")
    private String profileImageUrl;

    // Constructor vacío para GSON
    public UserProfileResponse() {
    }

    public UserProfileResponse(String username, boolean emailIsVerified, String profileImageUrl) {
        this.username = username;
        this.emailIsVerified = emailIsVerified;
        this.profileImageUrl = profileImageUrl;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isEmailVerified() {
        return emailIsVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailIsVerified = emailVerified;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    @Override
    public String toString() {
        return "UserProfileResponse{" +
                "username='" + username + '\'' +
                ", emailIsVerified=" + emailIsVerified +
                ", profileImageUrl='" + profileImageUrl + '\'' +
                '}';
    }
}
