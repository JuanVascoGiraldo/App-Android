package com.example.androidchatproject.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.androidchatproject.config.ApiConfig;
import com.example.androidchatproject.model.user.*;
import com.example.androidchatproject.utils.ErrorHandler;

import java.io.IOException;

/**
 * Cliente HTTP para endpoints de Usuario/Autenticación
 * Maneja los threads y callbacks en el hilo principal
 * Los errores se muestran automáticamente en Toast
 */
public class ApiHttpClientUser {
    
    private static final String TAG = "ApiHttpClient";
    private final HttpClient httpClient;
    private final Handler mainHandler;
    private final Context context;
    
    /**
     * Constructor
     * @param context Contexto para mostrar los Toast de errores
     */
    public ApiHttpClientUser(Context context) {
        this.httpClient = new HttpClient();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.context = context.getApplicationContext();
    }
    
    // ==================== CALLBACKS ====================
    
    public interface RegisterCallback {
        void onSuccess(RegisterResponse response);
        void onError(Exception error);
    }
    
    public interface LoginCallback {
        void onSuccess(AuthResponse response);
        void onError(Exception error);
    }
    
    public interface LogoutCallback {
        void onSuccess(LogoutResponse response);
        void onError(Exception error);
    }
    
    public interface SessionValidationCallback {
        void onSuccess(TokenValidationResponse response);
        void onError(Exception error);
    }
    
    public interface EmailVerificationCallback {
        void onSuccess(EmailVerificationResponse response);
        void onError(Exception error);
    }
    
    public interface EmailCodeValidationCallback {
        void onSuccess(EmailVerificationValidationResponse response);
        void onError(Exception error);
    }
    
    public interface UserProfileCallback {
        void onSuccess(UserProfileResponse response);
        void onError(Exception error);
    }
    
    public interface VerifyEmailCallback {
        void onSuccess(VerifyEmailResponse response);
        void onError(Exception error);
    }
    
    public interface ResendVerificationCallback {
        void onSuccess(ResendVerificationResponse response);
        void onError(Exception error);
    }
    
    public interface UploadProfileImageCallback {
        void onSuccess(UploadProfileImageResponse response);
        void onError(Exception error);
    }
    
    // ==================== ENDPOINTS ====================
    
    /**
     * Registrar nuevo usuario
     * POST /api/users/
     */
    public void registerUser(RegisterRequest request, RegisterCallback callback) {
        new Thread(() -> {
            try {
                String url = ApiConfig.BASE_URL + "api/users/";
                RegisterResponse response = httpClient.post(url, request, RegisterResponse.class, null);
                
                // Ejecutar callback en el hilo principal
                mainHandler.post(() -> callback.onSuccess(response));
                
            } catch (ApiException e) {
                Log.e(TAG, "Error API en registro", e);
                // Mostrar error en Toast automáticamente
                mainHandler.post(() -> {
                    ErrorHandler.showErrorToast(context, e.getErrorJson());
                    callback.onError(e);
                });
            } catch (IOException e) {
                Log.e(TAG, "Error de red en registro", e);
                mainHandler.post(() -> {
                    ErrorHandler.showNetworkError(context, e);
                    callback.onError(e);
                });
            }
        }).start();
    }
    
    /**
     * Login de usuario
     * POST /api/users/login
     */
    public void login(AuthRequest request, LoginCallback callback) {
        new Thread(() -> {
            try {
                String url = ApiConfig.BASE_URL + "api/users/login";
                AuthResponse response = httpClient.post(url, request, AuthResponse.class, null);
                
                mainHandler.post(() -> callback.onSuccess(response));
                
            } catch (ApiException e) {
                Log.e(TAG, "Error API en login", e);
                mainHandler.post(() -> {
                    ErrorHandler.showErrorToast(context, e.getErrorJson());
                    callback.onError(e);
                });
            } catch (IOException e) {
                Log.e(TAG, "Error de red en login", e);
                mainHandler.post(() -> {
                    ErrorHandler.showNetworkError(context, e);
                    callback.onError(e);
                });
            }
        }).start();
    }
    
    /**
     * Logout de usuario
     * POST /api/users//logout
     */
    public void logout(String authToken, LogoutCallback callback) {
        new Thread(() -> {
            try {
                String url = ApiConfig.BASE_URL + "api/users//logout";
                // POST sin body, solo con token
                LogoutResponse response = httpClient.post(url, new Object(), LogoutResponse.class, authToken);
                
                mainHandler.post(() -> callback.onSuccess(response));
                
            } catch (ApiException e) {
                Log.e(TAG, "Error API en logout", e);
                mainHandler.post(() -> {
                    ErrorHandler.showErrorToast(context, e.getErrorJson());
                    callback.onError(e);
                });
            } catch (IOException e) {
                Log.e(TAG, "Error de red en logout", e);
                mainHandler.post(() -> {
                    ErrorHandler.showNetworkError(context, e);
                    callback.onError(e);
                });
            }
        }).start();
    }
    
    /**
     * Validar sesión
     * POST /api/users/sessions/validate
     */
    public void validateSession(SessionValidationRequest request, SessionValidationCallback callback) {
        new Thread(() -> {
            try {
                String url = ApiConfig.BASE_URL + "api/users/sessions/validate";
                TokenValidationResponse response = httpClient.post(url, request, TokenValidationResponse.class, null);
                
                mainHandler.post(() -> callback.onSuccess(response));
                
            } catch (ApiException e) {
                Log.e(TAG, "Error API en validación de sesión", e);
                mainHandler.post(() -> {
                    ErrorHandler.showErrorToast(context, e.getErrorJson());
                    callback.onError(e);
                });
            } catch (IOException e) {
                Log.e(TAG, "Error de red en validación de sesión", e);
                mainHandler.post(() -> {
                    ErrorHandler.showNetworkError(context, e);
                    callback.onError(e);
                });
            }
        }).start();
    }
    
    /**
     * Enviar código de verificación de email
     * POST /api/users/email/verify
     */
    public void sendEmailVerification(String authToken, EmailVerificationCallback callback) {
        new Thread(() -> {
            try {
                String url = ApiConfig.BASE_URL + "api/users/email/verify";
                // POST sin body, solo con token
                EmailVerificationResponse response = httpClient.post(url, new Object(), EmailVerificationResponse.class, authToken);
                
                mainHandler.post(() -> callback.onSuccess(response));
                
            } catch (ApiException e) {
                Log.e(TAG, "Error API al enviar verificación de email", e);
                mainHandler.post(() -> {
                    ErrorHandler.showErrorToast(context, e.getErrorJson());
                    callback.onError(e);
                });
            } catch (IOException e) {
                Log.e(TAG, "Error de red al enviar verificación de email", e);
                mainHandler.post(() -> {
                    ErrorHandler.showNetworkError(context, e);
                    callback.onError(e);
                });
            }
        }).start();
    }
    
    /**
     * Validar código de verificación de email
     * POST /api/email/verify/validate
     */
    public void validateEmailCode(String authToken, EmailVerificationCode code, EmailCodeValidationCallback callback) {
        new Thread(() -> {
            try {
                String url = ApiConfig.BASE_URL + "api/users/email/verify/validate";
                EmailVerificationValidationResponse response = httpClient.post(url, code, EmailVerificationValidationResponse.class, authToken);
                
                mainHandler.post(() -> callback.onSuccess(response));
                
            } catch (ApiException e) {
                Log.e(TAG, "Error API al validar código de email", e);
                mainHandler.post(() -> {
                    ErrorHandler.showErrorToast(context, e.getErrorJson());
                    callback.onError(e);
                });
            } catch (IOException e) {
                Log.e(TAG, "Error de red al validar código de email", e);
                mainHandler.post(() -> {
                    ErrorHandler.showNetworkError(context, e);
                    callback.onError(e);
                });
            }
        }).start();
    }
    
    /**
     * Ejemplo de uso de GET (si tu API lo necesita)
     * Este es un ejemplo genérico
     */
    public <T> void get(String endpoint, Class<T> responseClass, String authToken, GenericCallback<T> callback) {
        new Thread(() -> {
            try {
                String url = ApiConfig.BASE_URL + endpoint;
                T response = httpClient.get(url, responseClass, authToken);
                
                mainHandler.post(() -> callback.onSuccess(response));
                
            } catch (ApiException e) {
                Log.e(TAG, "Error API en petición GET", e);
                mainHandler.post(() -> {
                    ErrorHandler.showErrorToast(context, e.getErrorJson());
                    callback.onError(e);
                });
            } catch (IOException e) {
                Log.e(TAG, "Error de red en petición GET", e);
                mainHandler.post(() -> {
                    ErrorHandler.showNetworkError(context, e);
                    callback.onError(e);
                });
            }
        }).start();
    }
    
    /**
     * Obtener perfil del usuario autenticado
     * GET /api/
     * 
     * @param token Token de autenticación
     * @param callback Callback con el perfil del usuario
     */
    public void getUserProfile(String token, UserProfileCallback callback) {
        new Thread(() -> {
            try {
                String url = ApiConfig.BASE_URL + "api/users/";
                UserProfileResponse response = httpClient.get(url, UserProfileResponse.class, token);
                
                // Ejecutar callback en el hilo principal
                mainHandler.post(() -> callback.onSuccess(response));
                
            } catch (ApiException e) {
                Log.e(TAG, "Error al obtener perfil de usuario", e);
                mainHandler.post(() -> {
                    ErrorHandler.showErrorToast(context, e.getErrorJson());
                    callback.onError(e);
                });
            } catch (IOException e) {
                Log.e(TAG, "Error de red al obtener perfil", e);
                mainHandler.post(() -> {
                    ErrorHandler.showNetworkError(context, e);
                    callback.onError(e);
                });
            }
        }).start();
    }
    
    /**
     * Verificar email con código de 6 dígitos
     * POST /api/verify-email
     */
    public void verifyEmail(String token, VerifyEmailRequest request, VerifyEmailCallback callback) {
        new Thread(() -> {
            try {
                String url = ApiConfig.BASE_URL + "api/users/email/verify/validate";
                VerifyEmailResponse response = httpClient.post(url, request, VerifyEmailResponse.class, token);
                
                mainHandler.post(() -> callback.onSuccess(response));
                
            } catch (ApiException e) {
                Log.e(TAG, "Error API en verificación de email", e);
                mainHandler.post(() -> {
                    ErrorHandler.showErrorToast(context, e.getErrorJson());
                    callback.onError(e);
                });
            } catch (IOException e) {
                Log.e(TAG, "Error de red en verificación de email", e);
                mainHandler.post(() -> {
                    ErrorHandler.showNetworkError(context, e);
                    callback.onError(e);
                });
            }
        }).start();
    }
    
    /**
     * Reenviar código de verificación
     * POST /api/resend-verification
     */
    public void resendVerification(String token, ResendVerificationCallback callback) {
        new Thread(() -> {
            try {
                String url = ApiConfig.BASE_URL + "api/users/email/verify";
                // POST sin body, solo con token
                ResendVerificationResponse response = httpClient.post(url, null, ResendVerificationResponse.class, token);
                
                mainHandler.post(() -> callback.onSuccess(response));
                
            } catch (ApiException e) {
                Log.e(TAG, "Error API en reenvío de código", e);
                mainHandler.post(() -> {
                    ErrorHandler.showErrorToast(context, e.getErrorJson());
                    callback.onError(e);
                });
            } catch (IOException e) {
                Log.e(TAG, "Error de red en reenvío de código", e);
                mainHandler.post(() -> {
                    ErrorHandler.showNetworkError(context, e);
                    callback.onError(e);
                });
            }
        }).start();
    }
    
    /**
     * Subir imagen de perfil
     * POST /api/users/upload/profile/image
     */
    public void uploadProfileImage(String token, byte[] imageBytes, String fileName, UploadProfileImageCallback callback) {
        new Thread(() -> {
            try {
                String url = ApiConfig.BASE_URL + "api/users/upload/profile/image";
                UploadProfileImageResponse response = httpClient.uploadFile(
                    url, 
                    imageBytes, 
                    fileName, 
                    "image",  // nombre del campo según el endpoint
                    UploadProfileImageResponse.class, 
                    token
                );
                
                mainHandler.post(() -> callback.onSuccess(response));
                
            } catch (ApiException e) {
                Log.e(TAG, "Error API en upload de imagen", e);
                mainHandler.post(() -> {
                    ErrorHandler.showErrorToast(context, e.getErrorJson());
                    callback.onError(e);
                });
            } catch (IOException e) {
                Log.e(TAG, "Error de red en upload de imagen", e);
                mainHandler.post(() -> {
                    ErrorHandler.showNetworkError(context, e);
                    callback.onError(e);
                });
            }
        }).start();
    }
    
    /**
     * Ejemplo de uso de PUT (si tu API lo necesita)
     * Este es un ejemplo genérico
     */
    public <T> void put(String endpoint, Object requestBody, Class<T> responseClass, String authToken, GenericCallback<T> callback) {
        new Thread(() -> {
            try {
                String url = ApiConfig.BASE_URL + endpoint;
                T response = httpClient.put(url, requestBody, responseClass, authToken);
                
                mainHandler.post(() -> callback.onSuccess(response));
                
            } catch (ApiException e) {
                Log.e(TAG, "Error API en petición PUT", e);
                mainHandler.post(() -> {
                    ErrorHandler.showErrorToast(context, e.getErrorJson());
                    callback.onError(e);
                });
            } catch (IOException e) {
                Log.e(TAG, "Error de red en petición PUT", e);
                mainHandler.post(() -> {
                    ErrorHandler.showNetworkError(context, e);
                    callback.onError(e);
                });
            }
        }).start();
    }
    
    // Callback genérico para GET y PUT
    public interface GenericCallback<T> {
        void onSuccess(T response);
        void onError(Exception error);
    }
}
