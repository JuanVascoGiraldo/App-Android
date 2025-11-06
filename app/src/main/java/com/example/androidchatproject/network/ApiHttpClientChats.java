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
                String url = ApiConfig.BASE_URL + "api/chats/id/" + chatId + "/";
                Log.d(TAG, "Getting chat detail from URL: " + url);
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
                String url = ApiConfig.BASE_URL + "api/chats/messages";
                Log.d(TAG, "Sending message to URL: " + url);
                
                // Usar multipart/form-data para consistencia con el endpoint
                java.util.Map<String, Object> formData = new java.util.HashMap<>();
                formData.put("chat_id", chatId);
                formData.put("content", content);
                
                SendMessageResponse response = httpClient.postMultipart(url, formData, SendMessageResponse.class, token);
                
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
     * POST api/chats/messages (multipart/form-data)
     * 
     * @param token Token de autenticación
     * @param chatId ID del chat
     * @param content Contenido del mensaje (opcional)
     * @param attachmentBytes Bytes del archivo adjunto
     * @param attachmentFileName Nombre del archivo adjunto
     * @param attachmentMimeType MIME type del archivo (opcional, se detecta por extensión si es null)
     * @param callback Callback con el resultado
     */
    public void sendMessageWithAttachment(String token, String chatId, String content, 
                                         byte[] attachmentBytes, String attachmentFileName, 
                                         String attachmentMimeType, SendMessageCallback callback) {
        new Thread(() -> {
            try {
                String url = ApiConfig.BASE_URL + "api/chats/messages";
                Log.d(TAG, "Sending message with attachment to URL: " + url);
                
                // Crear mapa con los datos del formulario
                java.util.Map<String, Object> formData = new java.util.HashMap<>();
                formData.put("chat_id", chatId);
                
                if (content != null && !content.isEmpty()) {
                    formData.put("content", content);
                }
                
                if (attachmentBytes != null && attachmentBytes.length > 0 && attachmentFileName != null) {
                    // Usar el mimeType proporcionado, o detectar por extensión si no está disponible
                    String mimeType = attachmentMimeType;
                    if (mimeType == null || mimeType.isEmpty()) {
                        mimeType = getMimeTypeFromFileName(attachmentFileName);
                    }
                    Log.d(TAG, "Attachment - FileName: " + attachmentFileName + ", MimeType: " + mimeType + ", Size: " + attachmentBytes.length);
                    formData.put("attachment", new HttpClient.FileData(attachmentBytes, attachmentFileName, mimeType));
                }
                
                // Usar el nuevo método postMultipart
                SendMessageResponse response = httpClient.postMultipart(
                    url, 
                    formData, 
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
    
    /**
     * Determina el MIME type basado en la extensión del archivo
     */
    private String getMimeTypeFromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "application/octet-stream";
        }
        
        String lower = fileName.toLowerCase();
        
        // Imágenes
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".bmp")) return "image/bmp";
        
        // PDFs
        if (lower.endsWith(".pdf")) return "application/pdf";
        
        // Audio
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".m4a")) return "audio/mp4";
        if (lower.endsWith(".aac")) return "audio/aac";
        if (lower.endsWith(".ogg")) return "audio/ogg";
        if (lower.endsWith(".flac")) return "audio/flac";
        
        // Videos
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".avi")) return "video/x-msvideo";
        if (lower.endsWith(".mov")) return "video/quicktime";
        
        // Documentos
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lower.endsWith(".txt")) return "text/plain";
        
        // Default
        return "application/octet-stream";
    }
}
