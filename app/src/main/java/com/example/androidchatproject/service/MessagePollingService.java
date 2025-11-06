package com.example.androidchatproject.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.androidchatproject.ChatDetailActivity;
import com.example.androidchatproject.R;
import com.example.androidchatproject.helpers.NotificationHelper;
import com.example.androidchatproject.model.chats.ChatItem;
import com.example.androidchatproject.model.chats.ChatsListResponse;
import com.example.androidchatproject.network.ApiHttpClientChats;
import com.example.androidchatproject.session.SessionManager;

import java.util.List;

/**
 * Servicio que ejecuta polling cada 30 segundos para detectar nuevos mensajes
 */
public class MessagePollingService extends Service {
    
    private static final String TAG = "MessagePollingService";
    private static final long POLLING_INTERVAL = 30000; // 30 segundos
    private static final String CHANNEL_ID = "chat_notifications";
    private static final String CHANNEL_NAME = "Mensajes de Chat";
    private static final int NOTIFICATION_ID_BASE = 1000;
    
    // Broadcast action para notificar a MainActivity
    public static final String ACTION_NEW_MESSAGES = "com.example.androidchatproject.NEW_MESSAGES";
    public static final String ACTION_CHATS_UPDATED = "com.example.androidchatproject.CHATS_UPDATED";
    
    private Handler handler;
    private Runnable pollingRunnable;
    private SessionManager sessionManager;
    private NotificationHelper notificationHelper;
    private ApiHttpClientChats apiClient;
    private boolean isRunning = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        
        sessionManager = new SessionManager(this);
        notificationHelper = new NotificationHelper(this);
        apiClient = new ApiHttpClientChats(this);
        handler = new Handler(Looper.getMainLooper());
        
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        
        // Para Android O+, debe ejecutarse como Foreground Service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (API 29+) requiere especificar el tipo de servicio
                startForeground(999, createForegroundNotification(), 
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(999, createForegroundNotification());
            }
        }
        
        if (!isRunning) {
            isRunning = true;
            startPolling();
        }
        
        return START_STICKY; // Reiniciar si el sistema mata el servicio
    }
    
    /**
     * Crear notificación para Foreground Service
     */
    private android.app.Notification createForegroundNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Chat activo")
                .setContentText("Buscando nuevos mensajes...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);
        
        return builder.build();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        stopPolling();
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // No bindeable
    }
    
    /**
     * Iniciar el polling periódico
     */
    private void startPolling() {
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                checkForNewMessages();
                
                // Programar siguiente ejecución
                if (isRunning) {
                    handler.postDelayed(this, POLLING_INTERVAL);
                }
            }
        };
        
        // Primera ejecución inmediata
        handler.post(pollingRunnable);
    }
    
    /**
     * Detener el polling
     */
    private void stopPolling() {
        isRunning = false;
        if (handler != null && pollingRunnable != null) {
            handler.removeCallbacks(pollingRunnable);
        }
    }
    
    /**
     * Verificar si hay mensajes nuevos
     */
    private void checkForNewMessages() {
        String token = sessionManager.getToken();
        String currentUserId = sessionManager.getUserId();
        
        if (token == null || currentUserId == null) {
            Log.d(TAG, "User not logged in, skipping polling");
            return;
        }
        
        Log.d(TAG, "Checking for new messages...");
        
        apiClient.getAllChats(token, new ApiHttpClientChats.ChatsListCallback() {
            @Override
            public void onSuccess(ChatsListResponse response) {
                if (response != null && response.getChats() != null) {
                    Log.d(TAG, "API Response toString: " + response.toString());
                    if (response.getChats().size() > 0) {
                        Log.d(TAG, "First chat toString: " + response.getChats().get(0).toString());
                    }
                    processChatsForNotifications(response.getChats(), currentUserId);
                }
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error getting chats: " + error.getMessage());
            }
        });
    }
    
    /**
     * Procesar chats y mostrar notificaciones si hay mensajes nuevos
     */
    private void processChatsForNotifications(List<ChatItem> chats, String currentUserId) {
        int newMessagesCount = 0;
        
        Log.d(TAG, "=== Processing " + chats.size() + " chats for notifications ===");
        Log.d(TAG, "Current user ID: " + currentUserId);
        
        for (ChatItem chat : chats) {
            String chatId = chat.getId();
            String lastMessageId = chat.getLastMessageId();
            String username = chat.getUsername();
            String userId = chat.getUser();
            
            Log.d(TAG, "--- Chat: " + chatId + " (user: " + username + ", userId: " + userId + ") ---");
            Log.d(TAG, "  lastMessageId from server: " + lastMessageId);
            
            // Solo procesar si hay un lastMessageId
            if (lastMessageId != null) {
                String lastSeenId = notificationHelper.getLastSeenMessageId(chatId);
                Log.d(TAG, "  lastSeenId from storage: " + lastSeenId);
                
                // Si no hay registro previo, guardar sin notificar (primera vez)
                if (lastSeenId == null) {
                    notificationHelper.saveLastSeenMessageId(chatId, lastMessageId);
                    Log.d(TAG, "  ✓ First message registered (no notification)");
                }
                // Si hay registro previo y es diferente, verificar si notificar
                else if (!lastSeenId.equals(lastMessageId)) {
                    Log.d(TAG, "  ✓ Message IDs are different! New message detected");
                    Log.d(TAG, "  Checking if message is from other user...");
                    Log.d(TAG, "  userId: " + userId + " vs currentUserId: " + currentUserId);
                    
                    // Solo notificar si el último mensaje NO es del usuario actual
                    if (!userId.equals(currentUserId)) {
                        Log.d(TAG, "  ✓ Message is from OTHER user - SHOWING NOTIFICATION");
                        showNotification(chatId, username, chat.getLastMessage(), userId, chat.getProfileImg());
                        newMessagesCount++;
                    } else {
                        Log.d(TAG, "  ✗ Message is from CURRENT user - skipping notification");
                    }
                    
                    // Actualizar el último mensaje visto (tanto si notificamos como si no)
                    notificationHelper.saveLastSeenMessageId(chatId, lastMessageId);
                    Log.d(TAG, "  Updated lastSeenId to: " + lastMessageId);
                } else {
                    Log.d(TAG, "  = Message IDs are the same - no changes");
                }
            } else {
                Log.d(TAG, "  ✗ No lastMessageId from server - skipping");
            }
        }
        
        Log.d(TAG, "=== Summary: " + newMessagesCount + " new notifications ===");
        
        if (newMessagesCount > 0) {
            Log.d(TAG, "Showed " + newMessagesCount + " new message notifications");
            
            // Enviar broadcast para notificar que hay mensajes nuevos
            Intent intent = new Intent(ACTION_NEW_MESSAGES);
            intent.putExtra("new_messages_count", newMessagesCount);
            sendBroadcast(intent);
            Log.d(TAG, "Broadcast sent: " + ACTION_NEW_MESSAGES);
        } else {
            Log.d(TAG, "No new messages to notify");
        }
        
        // Siempre enviar broadcast para actualizar la lista de chats
        Intent updateIntent = new Intent(ACTION_CHATS_UPDATED);
        sendBroadcast(updateIntent);
        Log.d(TAG, "Broadcast sent: " + ACTION_CHATS_UPDATED);
    }
    
    /**
     * Mostrar notificación de mensaje nuevo
     */
    private void showNotification(String chatId, String username, String message, String userId, String profileImg) {
        // Intent para abrir el chat cuando se toca la notificación
        Intent intent = new Intent(this, ChatDetailActivity.class);
        intent.putExtra("chat_id", chatId);
        intent.putExtra("user_id", userId);
        intent.putExtra("username", username);
        intent.putExtra("profile_img", profileImg);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                chatId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Construir la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Nuevo mensaje de " + username)
                .setContentText(message != null ? message : "Mensaje nuevo")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE);
        
        // Mostrar la notificación con un ID único por chat
        NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID_BASE + chatId.hashCode(), builder.build());
            Log.d(TAG, "Notification shown for chat: " + chatId + " from user: " + username);
        }
    }
    
    /**
     * Crear canal de notificaciones (requerido para Android O+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notificaciones de mensajes nuevos en chats");
            channel.enableVibration(true);
            channel.enableLights(true);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
            }
        }
    }
}
