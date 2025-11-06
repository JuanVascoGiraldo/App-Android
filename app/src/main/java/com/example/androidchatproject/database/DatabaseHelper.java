package com.example.androidchatproject.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    
    private static final String DATABASE_NAME = "ChatApp.db";
    private static final int DATABASE_VERSION = 2; // Incrementado para agregar nuevas columnas
    
    // Table name
    private static final String TABLE_SESSION = "session";
    
    // Column names
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TOKEN = "token";
    private static final String COLUMN_EXPIRATION_DATE = "expiration_date";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_CREATED_AT = "created_at";
    
    // Create table query
    private static final String CREATE_TABLE_SESSION = 
        "CREATE TABLE " + TABLE_SESSION + " (" +
        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_TOKEN + " TEXT NOT NULL, " +
        COLUMN_EXPIRATION_DATE + " TEXT, " +
        COLUMN_USER_ID + " TEXT, " +
        COLUMN_USERNAME + " TEXT, " +
        COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
        ")";
    
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SESSION);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSION);
        onCreate(db);
    }
    
    /**
     * Save token to database (for "remember me" functionality)
     */
    public boolean saveToken(String token, String expirationDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        // First, clear any existing tokens
        clearToken();
        
        ContentValues values = new ContentValues();
        values.put(COLUMN_TOKEN, token);
        values.put(COLUMN_EXPIRATION_DATE, expirationDate);
        
        long result = db.insert(TABLE_SESSION, null, values);
        return result != -1;
    }
    
    /**
     * Get saved token from database
     */
    public String getToken() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String token = null;
        
        try {
            String query = "SELECT " + COLUMN_TOKEN + " FROM " + TABLE_SESSION + 
                          " ORDER BY " + COLUMN_ID + " DESC LIMIT 1";
            cursor = db.rawQuery(query, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                token = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TOKEN));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return token;
    }
    
    /**
     * Get expiration date of saved token
     */
    public String getExpirationDate() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String expirationDate = null;
        
        try {
            String query = "SELECT " + COLUMN_EXPIRATION_DATE + " FROM " + TABLE_SESSION + 
                          " ORDER BY " + COLUMN_ID + " DESC LIMIT 1";
            cursor = db.rawQuery(query, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                expirationDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXPIRATION_DATE));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return expirationDate;
    }
    
    /**
     * Clear token from database
     */
    public void clearToken() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SESSION, null, null);
    }
    
    /**
     * Save userId to database
     */
    public boolean saveUserId(String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, userId);
        
        int rowsUpdated = db.update(TABLE_SESSION, values, null, null);
        return rowsUpdated > 0;
    }
    
    /**
     * Get saved userId from database
     */
    public String getUserId() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String userId = null;
        
        try {
            String query = "SELECT " + COLUMN_USER_ID + " FROM " + TABLE_SESSION + 
                          " ORDER BY " + COLUMN_ID + " DESC LIMIT 1";
            cursor = db.rawQuery(query, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(COLUMN_USER_ID);
                if (columnIndex != -1) {
                    userId = cursor.getString(columnIndex);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return userId;
    }
    
    /**
     * Save username to database
     */
    public boolean saveUsername(String username) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        
        int rowsUpdated = db.update(TABLE_SESSION, values, null, null);
        return rowsUpdated > 0;
    }
    
    /**
     * Get saved username from database
     */
    public String getUsername() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String username = null;
        
        try {
            String query = "SELECT " + COLUMN_USERNAME + " FROM " + TABLE_SESSION + 
                          " ORDER BY " + COLUMN_ID + " DESC LIMIT 1";
            cursor = db.rawQuery(query, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(COLUMN_USERNAME);
                if (columnIndex != -1) {
                    username = cursor.getString(columnIndex);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return username;
    }
    
    /**
     * Check if token exists in database
     */
    public boolean hasToken() {
        return getToken() != null;
    }
}
