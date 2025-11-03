package com.example.androidchatproject.helper;

import android.util.Log;
import android.util.Patterns;

/**
 * Helper para validaciones generales
 */
public class ValidationHelper {
    
    private static final String TAG = "ValidationHelper";
    
    /**
     * Validar email
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            Log.w(TAG, "❌ Email vacío");
            return false;
        }
        
        boolean isValid = Patterns.EMAIL_ADDRESS.matcher(email).matches();
        
        if (!isValid) {
            Log.w(TAG, "❌ Email inválido: " + email);
        }
        
        return isValid;
    }
    
    /**
     * Validar contraseña
     * Mínimo 8 caracteres, al menos una mayúscula, una minúscula y un número
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.isEmpty()) {
            Log.w(TAG, "❌ Contraseña vacía");
            return false;
        }
        
        if (password.length() < 8) {
            Log.w(TAG, "❌ Contraseña muy corta (mínimo 8 caracteres)");
            return false;
        }
        
        // Al menos una mayúscula
        if (!password.matches(".*[A-Z].*")) {
            Log.w(TAG, "❌ Contraseña debe contener al menos una mayúscula");
            return false;
        }
        
        // Al menos una minúscula
        if (!password.matches(".*[a-z].*")) {
            Log.w(TAG, "❌ Contraseña debe contener al menos una minúscula");
            return false;
        }
        
        // Al menos un número
        if (!password.matches(".*\\d.*")) {
            Log.w(TAG, "❌ Contraseña debe contener al menos un número");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validar código de verificación (6 dígitos)
     */
    public static boolean isValidVerificationCode(String code) {
        if (code == null || code.isEmpty()) {
            Log.w(TAG, "❌ Código vacío");
            return false;
        }
        
        if (code.length() != 6) {
            Log.w(TAG, "❌ Código debe tener 6 dígitos");
            return false;
        }
        
        if (!code.matches("\\d{6}")) {
            Log.w(TAG, "❌ Código debe contener solo números");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validar username
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.isEmpty()) {
            Log.w(TAG, "❌ Username vacío");
            return false;
        }
        
        if (username.length() < 3) {
            Log.w(TAG, "❌ Username muy corto (mínimo 3 caracteres)");
            return false;
        }
        
        if (username.length() > 20) {
            Log.w(TAG, "❌ Username muy largo (máximo 20 caracteres)");
            return false;
        }
        
        // Solo letras, números y guión bajo
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            Log.w(TAG, "❌ Username solo puede contener letras, números y guión bajo");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validar que dos contraseñas coincidan
     */
    public static boolean passwordsMatch(String password, String confirmPassword) {
        if (password == null || confirmPassword == null) {
            Log.w(TAG, "❌ Contraseñas nulas");
            return false;
        }
        
        boolean match = password.equals(confirmPassword);
        
        if (!match) {
            Log.w(TAG, "❌ Las contraseñas no coinciden");
        }
        
        return match;
    }
}
