package com.example.androidchatproject.model;

import com.google.gson.annotations.SerializedName;

/**
 * Modelo para errores de la API
 */
public class ApiError {
    
    @SerializedName("error_code")
    private int errorCode;
    
    @SerializedName("message")
    private String message;
    
    public ApiError() {
    }
    
    public ApiError(int errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }
    
    public int getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public String toString() {
        return "ApiError{" +
                "errorCode=" + errorCode +
                ", message='" + message + '\'' +
                '}';
    }
}
