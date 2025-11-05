package com.example.androidchatproject.model.user;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response para lista de usuarios
 */
public class UsersListResponse {
    
    @SerializedName("users")
    private List<UserListItem> users;
    
    public List<UserListItem> getUsers() {
        return users;
    }
    
    public void setUsers(List<UserListItem> users) {
        this.users = users;
    }
}
