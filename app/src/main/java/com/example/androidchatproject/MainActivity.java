package com.example.androidchatproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.androidchatproject.model.user.*;
import com.example.androidchatproject.network.ApiHttpClientUser;
import com.example.androidchatproject.session.SessionManager;

/**
 * MainActivity - Pantalla principal después del login
 */
public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    private ApiHttpClientUser apiHttpClient;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize SessionManager first
        sessionManager = new SessionManager(this);
        
        // 1. Intentar obtener token del Intent (desde Login/Register)
        Intent intent = getIntent();
        String token = intent.getStringExtra("TOKEN");
        String expirationDate = intent.getStringExtra("EXPIRATION_DATE");
        
        // 2. Si no viene en Intent, cargar desde SessionManager (SQLite o memoria)
        if (token == null || token.isEmpty()) {
            Log.d(TAG, "No token in Intent, loading from SessionManager...");
            
            if (!sessionManager.hasSession()) {
                Log.d(TAG, "No session found, navigating to Login");
                navigateToLogin();
                return;
            }
            
            token = sessionManager.getToken();
            expirationDate = sessionManager.getExpirationDate();
            Log.d(TAG, "Token loaded from SessionManager");
        } else {
            Log.d(TAG, "Token received from Intent, using it directly");
            // El token ya está guardado en SessionManager por LoginActivity/RegisterActivity
            // No necesitamos guardarlo de nuevo aquí para no sobrescribir la preferencia de "remember me"
        }
        
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Initialize API client
        apiHttpClient = new ApiHttpClientUser(this);
        
        // Validate the session token
        Log.d(TAG, "Validating session token...");
        validateSessionManual(token);
        
        // Obtener perfil del usuario
        getUserProfileExample(token);
    }
    
    // ==================== EJEMPLOS CON CONEXIONES MANUALES ====================
    
    /**
     * Example: Obtener perfil del usuario autenticado
     */
    private void getUserProfileExample(String token) {
        apiHttpClient.getUserProfile(token, new ApiHttpClientUser.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfileResponse response) {
                Log.d(TAG, "User Profile loaded successfully:");
                Log.d(TAG, "  Username: " + response.getUsername());
                Log.d(TAG, "  Email verified: " + response.isEmailVerified());
                Log.d(TAG, "  Profile image: " + response.getProfileImageUrl());
                
                Toast.makeText(MainActivity.this, 
                        "Bienvenido, " + response.getUsername() + "!", 
                        Toast.LENGTH_SHORT).show();
                
                // Descargar imagen de perfil si existe
                downloadProfileImageIfAvailable(response);
            }

            @Override
            public void onError(Exception error) {
                // El error ya se muestra automáticamente en un Toast por ErrorHandler
                Log.e(TAG, "User profile error", error);
            }
        });
    }
    
    /**
     * Sistema híbrido: Carga imagen con cache inteligente
     * 
     * Estrategia:
     * 1. Si cache es válido (< 7 días) → usar cache
     * 2. Si cache expiró → descargar nueva versión
     * 3. Si descarga falla → usar cache antiguo como fallback
     */
    private void downloadProfileImageIfAvailable(UserProfileResponse profile) {
        String imageUrl = profile.getProfileImageUrl();
        
        // Verificar si hay URL de imagen
        if (imageUrl == null || imageUrl.isEmpty()) {
            Log.d(TAG, "No profile image URL available");
            return;
        }
        
        String username = profile.getUsername();
        Log.d(TAG, "Loading profile image with cache strategy for: " + username);
        
        // Usar sistema híbrido con TTL de 7 días
        com.example.androidchatproject.utils.ImageDownloader.loadProfileImageWithCache(
            this,
            imageUrl,
            username,
            new com.example.androidchatproject.utils.ImageDownloader.DownloadCallback() {
                @Override
                public void onSuccess(java.io.File imageFile) {
                    long age = com.example.androidchatproject.utils.ImageDownloader
                            .getImageAgeInDays(MainActivity.this, 
                                com.example.androidchatproject.utils.ImageDownloader
                                    .generateProfileImageFileName(username));
                    
                    if (age == 0) {
                        // Recién descargada
                        Log.d(TAG, "✅ Profile image downloaded and cached");
                        Log.d(TAG, "  Path: " + imageFile.getAbsolutePath());
                        Log.d(TAG, "  Size: " + (imageFile.length() / 1024) + " KB");
                        
                        Toast.makeText(MainActivity.this, 
                                "Imagen de perfil descargada", 
                                Toast.LENGTH_SHORT).show();
                    } else {
                        // Cargada desde cache
                        Log.d(TAG, "✅ Profile image loaded from cache");
                        Log.d(TAG, "  Path: " + imageFile.getAbsolutePath());
                        Log.d(TAG, "  Age: " + age + " days");
                        Log.d(TAG, "  Size: " + (imageFile.length() / 1024) + " KB");
                        
                        // No mostrar toast para cache, solo log
                    }
                    
                    // Aquí Coria cargar la imagen en un ImageView
                    // loadImageIntoView(imageFile);
                }

                @Override
                public void onError(Exception error) {
                    Log.e(TAG, "❌ Error loading profile image", error);
                    
                    // Solo mostrar toast si es un error real (no cuando hay fallback)
                    if (!com.example.androidchatproject.utils.ImageDownloader
                            .imageExists(MainActivity.this, 
                                com.example.androidchatproject.utils.ImageDownloader
                                    .generateProfileImageFileName(username))) {
                        Toast.makeText(MainActivity.this, 
                                "No se pudo cargar la imagen de perfil", 
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
    }
    
    /**
     * Example: Registro con conexión manual HTTP
     */
    private void registerUserManualExample() {
        RegisterRequest request = new RegisterRequest(
                "john_doe",
                "john@example.com",
                "SecurePass123",
                "SecurePass123"
        );
        
        apiHttpClient.registerUser(request, new ApiHttpClientUser.RegisterCallback() {
            @Override
            public void onSuccess(RegisterResponse response) {
                if (response.isSuccess()) {
                    Toast.makeText(MainActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "User registered successfully (Manual HTTP)");
                }
            }
            
            @Override
            public void onError(Exception error) {
                // El error ya se muestra automáticamente en un Toast por ErrorHandler
                Log.e(TAG, "Registration error (Manual HTTP)", error);
            }
        });
    }
    
    /**
     * Example: Login con conexión manual HTTP
     */
    private void loginManualExample() {
        AuthRequest request = new AuthRequest(
                "john@example.com",
                "SecurePass123",
                true  // Remember me - will save to SQLite
        );
        
        apiHttpClient.login(request, new ApiHttpClientUser.LoginCallback() {
            @Override
            public void onSuccess(AuthResponse response) {
                String token = response.getJwt();
                String expirationDate = response.getExpirationDate();
                
                // Save token based on login_remember preference
                sessionManager.saveToken(token, expirationDate, request.isLoginRemember());
                
                Toast.makeText(MainActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Login successful (Manual HTTP). Token saved: " + 
                        (request.isLoginRemember() ? "SQLite" : "Memory"));
            }
            
            @Override
            public void onError(Exception error) {
                // El error ya se muestra automáticamente en un Toast por ErrorHandler
                Log.e(TAG, "Login error (Manual HTTP)", error);
            }
        });
    }
    
    /**
     * Example: Logout con conexión manual HTTP
     */
    private void logoutManualExample() {
        String token = sessionManager.getToken();
        if (token == null) {
            Toast.makeText(this, "No active session", Toast.LENGTH_SHORT).show();
            return;
        }
        
        apiHttpClient.logout(token, new ApiHttpClientUser.LogoutCallback() {
            @Override
            public void onSuccess(LogoutResponse response) {
                if (response.isSuccess()) {
                    // Clear session
                    sessionManager.clearSession();
                    Toast.makeText(MainActivity.this, "Logout successful!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Logout successful (Manual HTTP), session cleared");
                    
                    // Navigate to login screen
                    navigateToLogin();
                }
            }
            
            @Override
            public void onError(Exception error) {
                // El error ya se muestra automáticamente en un Toast por ErrorHandler
                Log.e(TAG, "Logout error (Manual HTTP)", error);
            }
        });
    }
    
    /**
     * Example: Validar sesión con conexión manual HTTP
     */
    private void validateSessionManual(String token) {
        SessionValidationRequest request = new SessionValidationRequest(token);
        
        apiHttpClient.validateSession(request, new ApiHttpClientUser.SessionValidationCallback() {
            @Override
            public void onSuccess(TokenValidationResponse response) {
                if (response.isValid()) {
                    Log.d(TAG, "Session is valid until: " + response.getValidUntil() + " (Manual HTTP)");
                    // Continue with app
                } else {
                    Log.d(TAG, "Session is invalid (Manual HTTP), clearing...");
                    sessionManager.clearSession();
                    // Navigate to login
                    navigateToLogin();
                }
            }
            
            @Override
            public void onError(Exception error) {
                // El error ya se muestra automáticamente en un Toast por ErrorHandler
                Log.e(TAG, "Session validation error (Manual HTTP)", error);
                // On error, also clear session and navigate to login
                sessionManager.clearSession();
                navigateToLogin();
            }
        });
    }
    
    /**
     * Example: Enviar código de verificación de email con conexión manual HTTP
     */
    private void sendEmailVerificationManualExample() {
        String token = sessionManager.getToken();
        if (token == null) {
            Toast.makeText(this, "No active session", Toast.LENGTH_SHORT).show();
            return;
        }
        
        apiHttpClient.sendEmailVerification(token, new ApiHttpClientUser.EmailVerificationCallback() {
            @Override
            public void onSuccess(EmailVerificationResponse response) {
                String validUntil = response.getValidUntil();
                Toast.makeText(MainActivity.this, "Verification code sent!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Verification code sent (Manual HTTP), valid until: " + validUntil);
            }
            
            @Override
            public void onError(Exception error) {
                // El error ya se muestra automáticamente en un Toast por ErrorHandler
                Log.e(TAG, "Email verification error (Manual HTTP)", error);
            }
        });
    }
    
    /**
     * Example: Validar código de verificación de email con conexión manual HTTP
     */
    private void validateEmailCodeManualExample(String code) {
        String token = sessionManager.getToken();
        if (token == null) {
            Toast.makeText(this, "No active session", Toast.LENGTH_SHORT).show();
            return;
        }
        
        EmailVerificationCode verificationCode = new EmailVerificationCode(code);
        
        apiHttpClient.validateEmailCode(token, verificationCode, 
                new ApiHttpClientUser.EmailCodeValidationCallback() {
            @Override
            public void onSuccess(EmailVerificationValidationResponse response) {
                if (response.isValid()) {
                    Toast.makeText(MainActivity.this, "Email verified successfully!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Email verification successful (Manual HTTP)");
                } else {
                    Toast.makeText(MainActivity.this, "Invalid verification code", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Invalid verification code (Manual HTTP)");
                }
            }
            
            @Override
            public void onError(Exception error) {
                // El error ya se muestra automáticamente en un Toast por ErrorHandler
                Log.e(TAG, "Email code validation error (Manual HTTP)", error);
            }
        });
    }
    
    /**
     * Navigate to Login Activity
     */
    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
