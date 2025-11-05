package com.example.androidchatproject.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.androidchatproject.model.chats.ChatDetailResponse;
import com.example.androidchatproject.model.chats.ChatItem;
import com.example.androidchatproject.model.chats.MessageResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper para cachear chats en SQLite
 * Maneja dos tipos de caché:
 * 1. Lista simple de chats (para la lista principal)
 * 2. Chats completos con mensajes (para la vista de detalle)
 */
public class ChatsCacheHelper extends SQLiteOpenHelper {
    
    private static final String TAG = "ChatsCacheHelper";
    private static final String DATABASE_NAME = "chats_cache.db";
    private static final int DATABASE_VERSION = 1;
    
    // Tabla de chats simples (lista)
    private static final String TABLE_CHATS = "chats";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USER = "user";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PROFILE_IMG = "profile_img";
    private static final String COLUMN_LAST_MESSAGE = "last_message";
    private static final String COLUMN_LAST_MESSAGE_TIME = "last_message_time";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";
    private static final String COLUMN_CACHED_AT = "cached_at";
    
    // Tabla de chats completos con mensajes
    private static final String TABLE_CHATS_DETAIL = "chats_detail";
    private static final String COLUMN_CHAT_ID = "chat_id";
    private static final String COLUMN_MESSAGES_JSON = "messages_json";
    private static final String COLUMN_DETAIL_CACHED_AT = "cached_at";
    
    private final Gson gson;
    
    public ChatsCacheHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.gson = new Gson();
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Crear tabla de chats simples
        String createChatsTable = "CREATE TABLE " + TABLE_CHATS + " (" +
                COLUMN_ID + " TEXT PRIMARY KEY, " +
                COLUMN_USER + " TEXT, " +
                COLUMN_USERNAME + " TEXT, " +
                COLUMN_PROFILE_IMG + " TEXT, " +
                COLUMN_LAST_MESSAGE + " TEXT, " +
                COLUMN_LAST_MESSAGE_TIME + " TEXT, " +
                COLUMN_CREATED_AT + " TEXT, " +
                COLUMN_UPDATED_AT + " TEXT, " +
                COLUMN_CACHED_AT + " INTEGER" +
                ")";
        db.execSQL(createChatsTable);
        
        // Crear tabla de chats completos (detalle con mensajes)
        String createChatsDetailTable = "CREATE TABLE " + TABLE_CHATS_DETAIL + " (" +
                COLUMN_CHAT_ID + " TEXT PRIMARY KEY, " +
                COLUMN_USER + " TEXT, " +
                COLUMN_USERNAME + " TEXT, " +
                COLUMN_PROFILE_IMG + " TEXT, " +
                COLUMN_MESSAGES_JSON + " TEXT, " +
                COLUMN_CREATED_AT + " TEXT, " +
                COLUMN_UPDATED_AT + " TEXT, " +
                COLUMN_DETAIL_CACHED_AT + " INTEGER" +
                ")";
        db.execSQL(createChatsDetailTable);
        
        Log.d(TAG, "Database created successfully");
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHATS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHATS_DETAIL);
        onCreate(db);
    }
    
    // ==================== CHATS SIMPLES (LISTA) ====================
    
    /**
     * Guardar lista de chats en caché
     * @param chats Lista de chats a guardar
     */
    public void cacheChats(List<ChatItem> chats) {
        if (chats == null || chats.isEmpty()) {
            Log.d(TAG, "No chats to cache");
            return;
        }
        
        SQLiteDatabase db = this.getWritableDatabase();
        long currentTime = System.currentTimeMillis();
        
        try {
            db.beginTransaction();
            
            // Limpiar tabla antes de insertar
            db.delete(TABLE_CHATS, null, null);
            
            // Insertar cada chat
            for (ChatItem chat : chats) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_ID, chat.getId());
                values.put(COLUMN_USER, chat.getUser());
                values.put(COLUMN_USERNAME, chat.getUsername());
                values.put(COLUMN_PROFILE_IMG, chat.getProfileImg());
                values.put(COLUMN_LAST_MESSAGE, chat.getLastMessage());
                values.put(COLUMN_LAST_MESSAGE_TIME, chat.getLastMessageTime());
                values.put(COLUMN_CREATED_AT, chat.getCreatedAt());
                values.put(COLUMN_UPDATED_AT, chat.getUpdatedAt());
                values.put(COLUMN_CACHED_AT, currentTime);
                
                db.insert(TABLE_CHATS, null, values);
            }
            
            db.setTransactionSuccessful();
            Log.d(TAG, chats.size() + " chats cached successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error caching chats", e);
        } finally {
            db.endTransaction();
        }
    }
    
    /**
     * Obtener lista de chats desde caché
     * @return Lista de chats cacheados
     */
    public List<ChatItem> getCachedChats() {
        List<ChatItem> chats = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = null;
        try {
            cursor = db.query(
                    TABLE_CHATS,
                    null,
                    null,
                    null,
                    null,
                    null,
                    COLUMN_CACHED_AT + " DESC"
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    ChatItem chat = new ChatItem();
                    chat.setId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                    chat.setUser(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER)));
                    chat.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)));
                    chat.setProfileImg(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROFILE_IMG)));
                    chat.setLastMessage(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_MESSAGE)));
                    chat.setLastMessageTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_MESSAGE_TIME)));
                    chat.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));
                    chat.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)));
                    
                    chats.add(chat);
                } while (cursor.moveToNext());
            }
            
            Log.d(TAG, chats.size() + " chats loaded from cache");
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading cached chats", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return chats;
    }
    
    /**
     * Verificar si hay chats en caché y si son recientes
     * @param maxAgeMillis Edad máxima del caché en milisegundos (ej: 24 horas = 86400000)
     * @return true si hay caché válido
     */
    public boolean hasCachedChats(long maxAgeMillis) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        
        try {
            cursor = db.query(
                    TABLE_CHATS,
                    new String[]{COLUMN_CACHED_AT},
                    null,
                    null,
                    null,
                    null,
                    COLUMN_CACHED_AT + " DESC",
                    "1"
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                long cachedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CACHED_AT));
                long age = System.currentTimeMillis() - cachedAt;
                return age < maxAgeMillis;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking cached chats", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return false;
    }
    
    // ==================== CHATS COMPLETOS (DETALLE CON MENSAJES) ====================
    
    /**
     * Guardar chat completo con mensajes en caché
     * @param chatDetail Chat detallado con mensajes
     */
    public void cacheChatDetail(ChatDetailResponse chatDetail) {
        if (chatDetail == null || chatDetail.getId() == null) {
            Log.d(TAG, "No chat detail to cache");
            return;
        }
        
        SQLiteDatabase db = this.getWritableDatabase();
        long currentTime = System.currentTimeMillis();
        
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_CHAT_ID, chatDetail.getId());
            values.put(COLUMN_USER, chatDetail.getUser());
            values.put(COLUMN_USERNAME, chatDetail.getUsername());
            values.put(COLUMN_PROFILE_IMG, chatDetail.getProfileImg());
            
            // Serializar lista de mensajes a JSON
            String messagesJson = gson.toJson(chatDetail.getMessages());
            values.put(COLUMN_MESSAGES_JSON, messagesJson);
            
            values.put(COLUMN_CREATED_AT, chatDetail.getCreatedAt());
            values.put(COLUMN_UPDATED_AT, chatDetail.getUpdatedAt());
            values.put(COLUMN_DETAIL_CACHED_AT, currentTime);
            
            // Insert or replace
            db.insertWithOnConflict(TABLE_CHATS_DETAIL, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            
            Log.d(TAG, "Chat detail cached: " + chatDetail.getId() + " with " + 
                    chatDetail.getMessages().size() + " messages");
            
        } catch (Exception e) {
            Log.e(TAG, "Error caching chat detail", e);
        }
    }
    
    /**
     * Obtener chat completo desde caché
     * @param chatId ID del chat
     * @return Chat detallado con mensajes o null si no existe
     */
    public ChatDetailResponse getCachedChatDetail(String chatId) {
        if (chatId == null || chatId.isEmpty()) {
            return null;
        }
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        
        try {
            cursor = db.query(
                    TABLE_CHATS_DETAIL,
                    null,
                    COLUMN_CHAT_ID + " = ?",
                    new String[]{chatId},
                    null,
                    null,
                    null
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                ChatDetailResponse chatDetail = new ChatDetailResponse();
                chatDetail.setId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHAT_ID)));
                chatDetail.setUser(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER)));
                chatDetail.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)));
                chatDetail.setProfileImg(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROFILE_IMG)));
                chatDetail.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));
                chatDetail.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)));
                
                // Deserializar JSON de mensajes
                String messagesJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGES_JSON));
                Type messageListType = new TypeToken<List<MessageResponse>>(){}.getType();
                List<MessageResponse> messages = gson.fromJson(messagesJson, messageListType);
                chatDetail.setMessages(messages);
                
                Log.d(TAG, "Chat detail loaded from cache: " + chatId + " with " + 
                        messages.size() + " messages");
                
                return chatDetail;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading cached chat detail", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return null;
    }
    
    /**
     * Verificar si existe un chat detallado en caché y si es reciente
     * @param chatId ID del chat
     * @param maxAgeMillis Edad máxima del caché en milisegundos
     * @return true si hay caché válido
     */
    public boolean hasCachedChatDetail(String chatId, long maxAgeMillis) {
        if (chatId == null || chatId.isEmpty()) {
            return false;
        }
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        
        try {
            cursor = db.query(
                    TABLE_CHATS_DETAIL,
                    new String[]{COLUMN_DETAIL_CACHED_AT},
                    COLUMN_CHAT_ID + " = ?",
                    new String[]{chatId},
                    null,
                    null,
                    null
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                long cachedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DETAIL_CACHED_AT));
                long age = System.currentTimeMillis() - cachedAt;
                return age < maxAgeMillis;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking cached chat detail", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return false;
    }
    
    // ==================== UTILIDADES ====================
    
    /**
     * Limpiar toda la caché de chats
     */
    public void clearAllCache() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.delete(TABLE_CHATS, null, null);
            db.delete(TABLE_CHATS_DETAIL, null, null);
            Log.d(TAG, "All cache cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing cache", e);
        }
    }
    
    /**
     * Limpiar solo la caché de chats simples
     */
    public void clearChatsCache() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.delete(TABLE_CHATS, null, null);
            Log.d(TAG, "Chats cache cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing chats cache", e);
        }
    }
    
    /**
     * Limpiar solo la caché de un chat específico
     */
    public void clearChatDetailCache(String chatId) {
        if (chatId == null || chatId.isEmpty()) {
            return;
        }
        
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.delete(TABLE_CHATS_DETAIL, COLUMN_CHAT_ID + " = ?", new String[]{chatId});
            Log.d(TAG, "Chat detail cache cleared: " + chatId);
        } catch (Exception e) {
            Log.e(TAG, "Error clearing chat detail cache", e);
        }
    }
    
    /**
     * Obtener el número de chats en caché
     */
    public int getCachedChatsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CHATS, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting cached chats count", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return 0;
    }
}
