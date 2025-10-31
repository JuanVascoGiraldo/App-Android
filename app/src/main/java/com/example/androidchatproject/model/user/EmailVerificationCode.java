package com.example.androidchatproject.model.user;


public class EmailVerificationCode {
    private String value; // código de 6 dígitos

    public EmailVerificationCode(String value) {
        this.value = value;
    }

    public String getValue() { return value; }
}
