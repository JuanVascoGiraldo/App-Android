package com.example.androidchatproject.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper para gestionar notificaciones y el seguimiento de último mensaje visto
 */
public class NotificationHelper {
    
    private static final String TAG = "NotificationHelper";
    private static final String PREFS_NAME = "notification_prefs";
    private static final String KEY_LAST_MESSAGE_ID_PREFIX = "last_msg_";
    
    private final SharedPreferences prefs;
    
    public NotificationHelper(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Guardar el último ID de mensaje visto para un chat
     * @param chatId ID del chat
     * @param messageId ID del último mensaje visto
     */
    public void saveLastSeenMessageId(String chatId, String messageId) {
        if (chatId == null || messageId == null) {
            return;
        }
        
        prefs.edit()
             .putString(KEY_LAST_MESSAGE_ID_PREFIX + chatId, messageId)
             .apply();
        
        Log.d(TAG, "Saved last seen message ID for chat " + chatId + ": " + messageId);
    }
    
    /**
     * Obtener el último ID de mensaje visto para un chat
     * @param chatId ID del chat
     * @return ID del último mensaje visto, o null si no existe
     */
    public String getLastSeenMessageId(String chatId) {
        if (chatId == null) {
            return null;
        }
        
        return prefs.getString(KEY_LAST_MESSAGE_ID_PREFIX + chatId, null);
    }
    
    /**
     * Verificar si hay un mensaje nuevo para un chat
     * @param chatId ID del chat
     * @param currentMessageId ID del mensaje actual del servidor
     * @return true si hay mensaje nuevo, false si no
     */
    public boolean hasNewMessage(String chatId, String currentMessageId) {
        if (chatId == null || currentMessageId == null) {
            return false;
        }
        
        String lastSeenId = getLastSeenMessageId(chatId);
        
        // Si no hay último mensaje visto, consideramos que no hay nuevo
        // (para evitar notificaciones en la primera carga)
        if (lastSeenId == null) {
            return false;
        }
        
        // Si los IDs son diferentes, hay mensaje nuevo
        boolean isNew = !currentMessageId.equals(lastSeenId);
        
        if (isNew) {
            Log.d(TAG, "New message detected for chat " + chatId + 
                      " - Last seen: " + lastSeenId + ", Current: " + currentMessageId);
        }
        
        return isNew;
    }
    
    /**
     * Obtener todos los chats con mensajes nuevos
     * @param chatsWithMessageIds Map de chatId -> currentMessageId
     * @return Map de chatId -> boolean (true si tiene mensaje nuevo)
     */
    public Map<String, Boolean> getChatsWithNewMessages(Map<String, String> chatsWithMessageIds) {
        Map<String, Boolean> result = new HashMap<>();
        
        for (Map.Entry<String, String> entry : chatsWithMessageIds.entrySet()) {
            String chatId = entry.getKey();
            String currentMessageId = entry.getValue();
            
            result.put(chatId, hasNewMessage(chatId, currentMessageId));
        }
        
        return result;
    }
    
    /**
     * Contar cuántos chats tienen mensajes nuevos
     * @param chatsWithMessageIds Map de chatId -> currentMessageId
     * @return Número de chats con mensajes nuevos
     */
    public int getNewMessagesCount(Map<String, String> chatsWithMessageIds) {
        int count = 0;
        
        for (Map.Entry<String, String> entry : chatsWithMessageIds.entrySet()) {
            if (hasNewMessage(entry.getKey(), entry.getValue())) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Limpiar el historial de mensajes vistos (útil para logout)
     */
    public void clearAllLastSeenMessages() {
        prefs.edit().clear().apply();
        Log.d(TAG, "Cleared all last seen message IDs");
    }
    
    /**
     * Limpiar el último mensaje visto de un chat específico
     * @param chatId ID del chat
     */
    public void clearLastSeenMessage(String chatId) {
        if (chatId == null) {
            return;
        }
        
        prefs.edit()
             .remove(KEY_LAST_MESSAGE_ID_PREFIX + chatId)
             .apply();
        
        Log.d(TAG, "Cleared last seen message ID for chat " + chatId);
    }
}
