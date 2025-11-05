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
}
