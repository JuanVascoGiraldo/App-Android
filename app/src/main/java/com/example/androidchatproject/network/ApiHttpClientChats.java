package com.example.androidchatproject.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.androidchatproject.config.ApiConfig;
import com.example.androidchatproject.model.chats.*;
import com.example.androidchatproject.utils.ErrorHandler;

import java.io.IOException;

/**
 * Cliente HTTP para endpoints de Chats
 * Maneja los threads y callbacks en el hilo principal
 */
public class ApiHttpClientChats {
    
    private static final String TAG = "ApiHttpClientChats";
    private final HttpClient httpClient;
    private final Handler mainHandler;
    private final Context context;
    
    /**
     * Constructor
     * @param context Contexto para mostrar los Toast de errores
     */
    public ApiHttpClientChats(Context context) {
        this.httpClient = new HttpClient();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.context = context.getApplicationContext();
    }
    
    // ==================== CALLBACKS ====================
    
    /**
     * Callback para obtener lista de chats
     */
    public interface ChatsListCallback {
        void onSuccess(ChatsListResponse response);
        void onError(Exception error);
    }
    
    /**
     * Callback para crear un chat
     */
    public interface CreateChatCallback {
        void onSuccess(CreateChatResponse response);
        void onError(Exception error);
    }
    
    /**
     * Callback para obtener detalle de un chat con sus mensajes
     */
    public interface ChatDetailCallback {
        void onSuccess(ChatDetailResponse response);
        void onError(Exception error);
    }
    
    /**
     * Callback para enviar un mensaje
     */
    public interface SendMessageCallback {
        void onSuccess(SendMessageResponse response);
        void onError(Exception error);
    }
    
    // ==================== MÉTODOS ====================
    
    /**
     * Obtener todos los chats del usuario autenticado
     * GET api/chats/all/
     */
    public void getAllChats(String token, ChatsListCallback callback) {
        new Thread(() -> {
            try {
                String url = ApiConfig.BASE_URL + "api/chats/all/";
                ChatsListResponse response = httpClient.get(url, ChatsListResponse.class, token);
                
                Log.d(TAG, "Chats list retrieved successfully");
                
                mainHandler.post(() -> callback.onSuccess(response));
                
            } catch (ApiException e) {
                Log.e(TAG, "API Error getting chats list", e);
                mainHandler.post(() -> {
                    ErrorHandler.showErrorToast(context, e.getErrorJson());
                    callback.onError(e);
                });
            } catch (IOException e) {
                Log.e(TAG, "Network error getting chats list", e);
                mainHandler.post(() -> {
                    ErrorHandler.showNetworkError(context, e);
                    callback.onError(e);
                });
            }
        }).start();
    }
    
    /**
     * Crear un nuevo chat con un usuario
     * POST api/chats/
     * 
     * @param token Token de autenticación
     * @param userId ID del usuario con quien iniciar el chat
     * @param content Contenido del primer mensaje
     * @param callback Callback con el resultado
     */
    public void createChat(String token, String userId, String content, CreateChatCallback callback) {
        new Thread(() -> {
            try {
                String url = ApiConfig.BASE_URL + "api/chats/";
                CreateChatRequest request = new CreateChatRequest(userId, content);
                CreateChatResponse response = httpClient.post(url, request, CreateChatResponse.class, token);
                
                Log.d(TAG, "Chat created successfully");
                
                mainHandler.post(() -> callback.onSuccess(response));
                
            } catch (ApiException e) {
                Log.e(TAG, "API Error creating chat", e);
                mainHandler.post(() -> {
                    ErrorHandler.showErrorToast(context, e.getErrorJson());
                    callback.onError(e);
                });
            } catch (IOException e) {
                Log.e(TAG, "Network error creating chat", e);
                mainHandler.post(() -> {
                    ErrorHandler.showNetworkError(context, e);
                    callback.onError(e);
                });
            }
        }).start();
    }
    
    /**
     * Obtener detalle de un chat con todos sus mensajes
     * GET api/chats/id/{chat_id}
     * 
     * @param token Token de autenticación
     * @param chatId ID del chat
     * @param callback Callback con el resultado
     */
    public void getChatById(String token, String chatId, ChatDetailCallback callback) {
        new Thread(() -> {
            try {
                String url = ApiConfig.BASE_URL + "api/chats/id/" + chatId;
                ChatDetailResponse response = httpClient.get(url, ChatDetailResponse.class, token);
                
                Log.d(TAG, "Chat detail retrieved successfully: " + response.getMessages().size() + " messages");
                
                mainHandler.post(() -> callback.onSuccess(response));
                
            } catch (ApiException e) {
                Log.e(TAG, "API Error getting chat detail", e);
                mainHandler.post(() -> {
                    ErrorHandler.showErrorToast(context, e.getErrorJson());
                    callback.onError(e);
                });
            } catch (IOException e) {
                Log.e(TAG, "Network error getting chat detail", e);
                mainHandler.post(() -> {
                    ErrorHandler.showNetworkError(context, e);
                    callback.onError(e);
                });
            }
        }).start();
    }
    
    /**
     * Enviar un mensaje de texto en un chat existente
     * POST api/chats/messages/
     * 
     * @param token Token de autenticación
     * @param chatId ID del chat
     * @param content Contenido del mensaje (texto)
     * @param callback Callback con el resultado
     */
    public void sendMessage(String token, String chatId, String content, SendMessageCallback callback) {
        new Thread(() -> {
            try {
                String url = ApiConfig.BASE_URL + "api/chats/messages/";
                SendMessageRequest request = new SendMessageRequest(chatId, content);
                SendMessageResponse response = httpClient.post(url, request, SendMessageResponse.class, token);
                
                Log.d(TAG, "Message sent successfully");
                
                mainHandler.post(() -> callback.onSuccess(response));
                
            } catch (ApiException e) {
                Log.e(TAG, "API Error sending message", e);
                mainHandler.post(() -> {
                    ErrorHandler.showErrorToast(context, e.getErrorJson());
                    callback.onError(e);
                });
            } catch (IOException e) {
                Log.e(TAG, "Network error sending message", e);
                mainHandler.post(() -> {
                    ErrorHandler.showNetworkError(context, e);
                    callback.onError(e);
                });
            }
        }).start();
    }
    
    /**
     * Enviar un mensaje con archivo adjunto en un chat existente
     * POST api/chats/messages/ (multipart/form-data)
     * 
     * @param token Token de autenticación
     * @param chatId ID del chat
     * @param content Contenido del mensaje (opcional)
     * @param attachmentBytes Bytes del archivo adjunto
     * @param attachmentFileName Nombre del archivo adjunto
     * @param callback Callback con el resultado
     */
    public void sendMessageWithAttachment(String token, String chatId, String content, 
                                         byte[] attachmentBytes, String attachmentFileName, 
                                         SendMessageCallback callback) {
        new Thread(() -> {
            try {
                String url = ApiConfig.BASE_URL + "api/chats/messages/";
                
                // Usar el método multipart del HttpClient
                SendMessageResponse response = httpClient.postMultipartWithChatMessage(
                    url, 
                    chatId, 
                    content, 
                    attachmentBytes, 
                    attachmentFileName, 
                    SendMessageResponse.class, 
                    token
                );
                
                Log.d(TAG, "Message with attachment sent successfully");
                
                mainHandler.post(() -> callback.onSuccess(response));
                
            } catch (ApiException e) {
                Log.e(TAG, "API Error sending message with attachment", e);
                mainHandler.post(() -> {
                    ErrorHandler.showErrorToast(context, e.getErrorJson());
                    callback.onError(e);
                });
            } catch (IOException e) {
                Log.e(TAG, "Network error sending message with attachment", e);
                mainHandler.post(() -> {
                    ErrorHandler.showNetworkError(context, e);
                    callback.onError(e);
                });
            }
        }).start();
    }
}
