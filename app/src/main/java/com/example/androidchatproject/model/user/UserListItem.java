package com.example.androidchatproject.model.user;

import com.google.gson.annotations.SerializedName;

/**
 * Modelo para usuario en lista de búsqueda
 */
public class UserListItem {
    
    @SerializedName("username")
    private String username;
    
    @SerializedName("user_id")
    private String userId;
    
    @SerializedName("profile_image_url")
    private String profileImageUrl;
    
    // Constructor vacío para SQLite
    public UserListItem() {
    }
    
    // Constructor completo
    public UserListItem(String username, String userId, String profileImageUrl) {
        this.username = username;
        this.userId = userId;
        this.profileImageUrl = profileImageUrl;
    }
    
    // Getters
    public String getUsername() {
        return username;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getProfileImageUrl() {
        return profileImageUrl;
    }
    
    // Setters
    public void setUsername(String username) {
        this.username = username;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
    
    @Override
    public String toString() {
        return "UserListItem{" +
                "username='" + username + '\'' +
                ", userId='" + userId + '\'' +
                ", profileImageUrl='" + profileImageUrl + '\'' +
                '}';
    }
}
