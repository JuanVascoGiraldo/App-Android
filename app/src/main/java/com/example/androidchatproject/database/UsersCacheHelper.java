package com.example.androidchatproject.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.androidchatproject.model.user.UserListItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper para base de datos de caché de usuarios
 */
public class UsersCacheHelper extends SQLiteOpenHelper {
    
    private static final String TAG = "UsersCacheHelper";
    private static final String DATABASE_NAME = "users_cache.db";
    private static final int DATABASE_VERSION = 1;
    
    // Tabla de usuarios
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PROFILE_IMAGE_URL = "profile_image_url";
    private static final String COLUMN_CACHED_AT = "cached_at";
    
    // SQL para crear tabla
    private static final String CREATE_TABLE_USERS =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_USER_ID + " TEXT PRIMARY KEY," +
                    COLUMN_USERNAME + " TEXT NOT NULL," +
                    COLUMN_PROFILE_IMAGE_URL + " TEXT," +
                    COLUMN_CACHED_AT + " INTEGER NOT NULL" +
                    ")";
    
    public UsersCacheHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
        Log.d(TAG, "[SUCCESS] Tabla de usuarios creada");
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
        Log.d(TAG, "🔄 Base de datos actualizada");
    }
    
    /**
     * Guardar lista de usuarios en caché
     */
    public void cacheUsers(List<UserListItem> users) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        // Limpiar tabla antes de insertar nuevos datos
        db.delete(TABLE_USERS, null, null);
        
        long currentTime = System.currentTimeMillis();
        int savedCount = 0;
        
        db.beginTransaction();
        try {
            for (UserListItem user : users) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_USER_ID, user.getUserId());
                values.put(COLUMN_USERNAME, user.getUsername());
                values.put(COLUMN_PROFILE_IMAGE_URL, user.getProfileImageUrl());
                values.put(COLUMN_CACHED_AT, currentTime);
                
                long result = db.insert(TABLE_USERS, null, values);
                if (result != -1) {
                    savedCount++;
                }
            }
            db.setTransactionSuccessful();
            Log.d(TAG, "[SUCCESS] " + savedCount + " usuarios guardados en caché");
        } catch (Exception e) {
            Log.e(TAG, "[ERROR] Error al guardar usuarios en caché", e);
        } finally {
            db.endTransaction();
        }
    }
    
    /**
     * Obtener todos los usuarios desde caché
     */
    public List<UserListItem> getCachedUsers() {
        List<UserListItem> users = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(
                TABLE_USERS,
                null,
                null,
                null,
                null,
                null,
                COLUMN_USERNAME + " ASC"
        );
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID));
                String username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME));
                String profileImageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROFILE_IMAGE_URL));
                
                UserListItem user = new UserListItem(username, userId, profileImageUrl);
                users.add(user);
            }
            cursor.close();
        }
        
        Log.d(TAG, "📦 " + users.size() + " usuarios cargados desde caché");
        return users;
    }
    
    /**
     * Verificar si hay datos en caché
     */
    public boolean hasCache() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_USERS, null);
        
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        
        return count > 0;
    }
    
    /**
     * Obtener edad del caché en milisegundos
     */
    public long getCacheAge() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COLUMN_CACHED_AT + " FROM " + TABLE_USERS + " LIMIT 1",
                null
        );
        
        long cacheTime = 0;
        if (cursor.moveToFirst()) {
            cacheTime = cursor.getLong(0);
        }
        cursor.close();
        
        if (cacheTime == 0) {
            return Long.MAX_VALUE; // Sin caché
        }
        
        return System.currentTimeMillis() - cacheTime;
    }
    
    /**
     * Verificar si el caché es válido (menos de 24 horas)
     */
    public boolean isCacheValid() {
        long cacheAge = getCacheAge();
        long maxAge = 24 * 60 * 60 * 1000; // 24 horas
        return cacheAge < maxAge;
    }
    
    /**
     * Limpiar caché
     */
    public void clearCache() {
        SQLiteDatabase db = this.getWritableDatabase();
        int deleted = db.delete(TABLE_USERS, null, null);
        Log.d(TAG, "🗑️ Caché limpiado: " + deleted + " usuarios eliminados");
    }
}
