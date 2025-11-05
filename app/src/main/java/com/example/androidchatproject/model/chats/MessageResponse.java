package com.example.androidchatproject.model.chats;

import com.google.gson.annotations.SerializedName;

/**
 * Modelo para un mensaje individual
 */
public class MessageResponse {
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("sender_id")
    private String senderId;
    
    @SerializedName("content")
    private String content;
    
    @SerializedName("attachment_url")
    private String attachmentUrl;
    
    @SerializedName("is_deleted")
    private boolean isDeleted;
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("updated_at")
    private String updatedAt;
    
    // Constructor vacío
    public MessageResponse() {
    }
    
    // Constructor completo
    public MessageResponse(String id, String senderId, String content, String attachmentUrl,
                          boolean isDeleted, String createdAt, String updatedAt) {
        this.id = id;
        this.senderId = senderId;
        this.content = content;
        this.attachmentUrl = attachmentUrl;
        this.isDeleted = isDeleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    public String getContent() {
        return content;
    }
    
    public String getAttachmentUrl() {
        return attachmentUrl;
    }
    
    public boolean isDeleted() {
        return isDeleted;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public String getUpdatedAt() {
        return updatedAt;
    }
    
    // Setters
    public void setId(String id) {
        this.id = id;
    }
    
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }
    
    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return "MessageResponse{" +
                "id='" + id + '\'' +
                ", senderId='" + senderId + '\'' +
                ", content='" + content + '\'' +
                ", attachmentUrl='" + attachmentUrl + '\'' +
                ", isDeleted=" + isDeleted +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                '}';
    }
}
