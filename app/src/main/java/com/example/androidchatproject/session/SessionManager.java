package com.example.androidchatproject.session;

import android.content.Context;
import com.example.androidchatproject.database.DatabaseHelper;

/**
 * SessionManager handles token lifecycle:
 * - If login_remember is true, saves token in SQLite
 * - If login_remember is false, keeps token in memory only
 */
public class SessionManager {
    
    private DatabaseHelper databaseHelper;
    private String memoryToken;  // For non-persistent sessions
    private String memoryExpirationDate;
    private String memoryUsername;  // For offline mode
    
    public SessionManager(Context context) {
        this.databaseHelper = new DatabaseHelper(context);
    }
    
    /**
     * Save token (persistent if rememberMe is true, in-memory otherwise)
     */
    public void saveToken(String token, String expirationDate, boolean rememberMe) {
        if (rememberMe) {
            // Save to database
            android.util.Log.d("SessionManager", "Saving token to SQLite database (persistent)");
            databaseHelper.saveToken(token, expirationDate);
            // Clear memory token
            memoryToken = null;
            memoryExpirationDate = null;
        } else {
            // Keep in memory only
            android.util.Log.d("SessionManager", "Saving token to memory (temporary)");
            memoryToken = token;
            memoryExpirationDate = expirationDate;
            // Clear database token
            databaseHelper.clearToken();
        }
    }
    
    /**
     * Get current token (from database or memory)
     */
    public String getToken() {
        // First check database
        String token = databaseHelper.getToken();
        if (token != null) {
            android.util.Log.d("SessionManager", "Token loaded from SQLite database");
            return token;
        }
        // Then check memory
        if (memoryToken != null) {
            android.util.Log.d("SessionManager", "Token loaded from memory");
        } else {
            android.util.Log.d("SessionManager", "No token found in database or memory");
        }
        return memoryToken;
    }
    
    /**
     * Get expiration date
     */
    public String getExpirationDate() {
        // First check database
        String expirationDate = databaseHelper.getExpirationDate();
        if (expirationDate != null) {
            return expirationDate;
        }
        // Then check memory
        return memoryExpirationDate;
    }
    
    /**
     * Check if user has an active session
     */
    public boolean hasSession() {
        return getToken() != null;
    }
    
    /**
     * Check if session is remembered (saved in database)
     */
    public boolean isSessionRemembered() {
        return databaseHelper.hasToken();
    }
    
    /**
     * Clear session (both database and memory)
     */
    public void clearSession() {
        databaseHelper.clearToken();
        memoryToken = null;
        memoryExpirationDate = null;
    }
    
    /**
     * Save username for offline mode
     */
    public void saveUsername(String username) {
        this.memoryUsername = username;
        android.util.Log.d("SessionManager", "Username saved: " + username);
    }
    
    /**
     * Get username for offline mode
     */
    public String getUsername() {
        return memoryUsername;
    }
    
    /**
     * Get DatabaseHelper instance if needed for advanced operations
     */
    public DatabaseHelper getDatabaseHelper() {
        return databaseHelper;
    }
}
