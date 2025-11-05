package com.example.androidchatproject.model.chats;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Modelo de respuesta para la lista de chats
 */
public class ChatsListResponse {
    
    @SerializedName("chats")
    private List<ChatItem> chats;
    
    // Constructor vacío
    public ChatsListResponse() {
    }
    
    // Constructor con parámetro
    public ChatsListResponse(List<ChatItem> chats) {
        this.chats = chats;
    }
    
    // Getter
    public List<ChatItem> getChats() {
        return chats;
    }
    
    // Setter
    public void setChats(List<ChatItem> chats) {
        this.chats = chats;
    }
    
    @Override
    public String toString() {
        return "ChatsListResponse{" +
                "chats=" + (chats != null ? chats.size() + " items" : "null") +
                '}';
    }
}
