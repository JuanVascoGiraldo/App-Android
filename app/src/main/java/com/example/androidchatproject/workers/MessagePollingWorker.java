package com.example.androidchatproject.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.androidchatproject.ChatDetailActivity;
import com.example.androidchatproject.R;
import com.example.androidchatproject.network.ApiHttpClientChats;
import com.example.androidchatproject.session.SessionManager;
import com.example.androidchatproject.helpers.NotificationHelper;
import com.example.androidchatproject.model.chats.ChatItem;
import com.example.androidchatproject.model.chats.ChatsListResponse;

import java.util.List;

/**
 * Worker que se ejecuta periódicamente para verificar nuevos mensajes
 */
public class MessagePollingWorker extends Worker {
    
    private static final String TAG = "MessagePollingWorker";
    private static final String CHANNEL_ID = "chat_notifications";
    private static final String CHANNEL_NAME = "Mensajes de Chat";
    private static final int NOTIFICATION_ID_BASE = 1000;
    
    private final Context context;
    private final SessionManager sessionManager;
    private final NotificationHelper notificationHelper;
    private final ApiHttpClientChats apiClient;
    
    public MessagePollingWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
        this.sessionManager = new SessionManager(context);
        this.notificationHelper = new NotificationHelper(context);
        this.apiClient = new ApiHttpClientChats(context);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting message polling");
        
        // Verificar si el usuario está logueado
        String token = sessionManager.getToken();
        String currentUserId = sessionManager.getUserId();
        
        if (token == null || currentUserId == null) {
            Log.d(TAG, "User not logged in, skipping polling");
            return Result.success();
        }
        
        // Obtener la lista de chats del servidor
        try {
            // Nota: Esto debe ejecutarse de forma síncrona en el Worker
            // Necesitaremos usar un CountDownLatch o similar para esperar la respuesta
            final Object lock = new Object();
            final boolean[] completed = {false};
            final boolean[] success = {false};
            
            apiClient.getAllChats(token, new ApiHttpClientChats.ChatsListCallback() {
                @Override
                public void onSuccess(ChatsListResponse response) {
                    if (response != null && response.getChats() != null) {
                        checkForNewMessages(response.getChats(), currentUserId);
                        success[0] = true;
                    }
                    synchronized (lock) {
                        completed[0] = true;
                        lock.notify();
                    }
                }
                
                @Override
                public void onError(Exception error) {
                    Log.e(TAG, "Error getting chats: " + error.getMessage());
                    synchronized (lock) {
                        completed[0] = true;
                        lock.notify();
                    }
                }
            });
            
            // Esperar hasta 30 segundos por la respuesta
            synchronized (lock) {
                if (!completed[0]) {
                    lock.wait(30000);
                }
            }
            
            return success[0] ? Result.success() : Result.retry();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in polling worker", e);
            return Result.retry();
        }
    }
    
    /**
     * Verificar si hay mensajes nuevos y mostrar notificaciones
     */
    private void checkForNewMessages(List<ChatItem> chats, String currentUserId) {
        if (chats == null || chats.isEmpty()) {
            return;
        }
        
        for (ChatItem chat : chats) {
            String chatId = chat.getId();
            String lastMessageId = chat.getLastMessageId();
            String username = chat.getUsername();
            String userId = chat.getUser();
            
            // Solo notificar si el último mensaje no es del usuario actual
            // y si hay un mensaje nuevo
            if (lastMessageId != null && !userId.equals(currentUserId)) {
                if (notificationHelper.hasNewMessage(chatId, lastMessageId)) {
                    showNotification(chatId, username, chat.getLastMessage(), userId, chat.getProfileImg());
                    
                    // Actualizar el último mensaje visto para este chat
                    notificationHelper.saveLastSeenMessageId(chatId, lastMessageId);
                }
            }
        }
    }
    
    /**
     * Mostrar notificación de mensaje nuevo
     */
    private void showNotification(String chatId, String username, String message, String userId, String profileImg) {
        createNotificationChannel();
        
        // Intent para abrir el chat cuando se toca la notificación
        Intent intent = new Intent(context, ChatDetailActivity.class);
        intent.putExtra("chat_id", chatId);
        intent.putExtra("user_id", userId);
        intent.putExtra("username", username);
        intent.putExtra("profile_img", profileImg);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                chatId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Construir la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Necesitarás crear este ícono
                .setContentTitle("Nuevo mensaje de " + username)
                .setContentText(message != null ? message : "Mensaje nuevo")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        
        // Mostrar la notificación con un ID único por chat
        NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
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
            
            NotificationManager notificationManager = 
                    context.getSystemService(NotificationManager.class);
            
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
