package com.example.androidchatproject.model.user;

import com.google.gson.annotations.SerializedName;

/**
 * Request para verificar email con código de 6 dígitos
 * POST /api/verify-email
 */
public class VerifyEmailRequest {
    
    @SerializedName("value")
    private String value;

    public VerifyEmailRequest(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
