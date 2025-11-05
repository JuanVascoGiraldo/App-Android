package com.example.androidchatproject;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.ListView;

import com.example.androidchatproject.adapter.ChatsAdapter;
import com.example.androidchatproject.model.chats.ChatItem;
import com.example.androidchatproject.model.chats.ChatsListResponse;
import com.example.androidchatproject.model.user.*;
import com.example.androidchatproject.network.ApiHttpClientChats;
import com.example.androidchatproject.network.ApiHttpClientUser;
import com.example.androidchatproject.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * MainActivity - Pantalla principal después del login
 */
public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    private static final int MAX_IMAGE_SIZE = 1024 * 1024 * 10; // 1MB
    
    private ApiHttpClientUser apiHttpClient;
    private ApiHttpClientChats apiHttpClientChats;
    private SessionManager sessionManager;
    private String currentToken;
    private boolean isComingFromLogin = false;
    
    // UI Components
    private ImageView profileImageView;
    private TextView welcomeTextView;
    private FloatingActionButton uploadImageButton;
    private MaterialButton searchUsersButton;
    private ListView chatsListView;
    
    // Chats
    private ChatsAdapter chatsAdapter;
    private List<ChatItem> allChats;
    
    // Image picker launcher
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    
    // Permission launcher
    private ActivityResultLauncher<String> requestPermissionLauncher;

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
            isComingFromLogin = false; // Viene desde sesión guardada
            
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
            isComingFromLogin = true; // Viene desde login/register reciente
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
        
        // Store token for later use
        currentToken = token;
        
        // Initialize UI components
        profileImageView = findViewById(R.id.profileImageView);
        welcomeTextView = findViewById(R.id.welcomeTextView);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        searchUsersButton = findViewById(R.id.searchUsersButton);
        chatsListView = findViewById(R.id.chatsListView);
        
        // Initialize API clients
        apiHttpClient = new ApiHttpClientUser(this);
        apiHttpClientChats = new ApiHttpClientChats(this);
        
        // Initialize chats adapter
        allChats = new ArrayList<>();
        chatsAdapter = new ChatsAdapter(this, allChats);
        chatsListView.setAdapter(chatsAdapter);
        
        // Setup chats click listener
        chatsListView.setOnItemClickListener((parent, view, position, id) -> {
            ChatItem chat = chatsAdapter.getItem(position);
            Toast.makeText(this, "Chat con: " + chat.getUsername(), Toast.LENGTH_SHORT).show();
            // TODO: Navegar a pantalla de chat detallado
        });
        
        // Setup image picker
        setupImagePicker();
        
        // Setup upload button - verificar permisos primero
        uploadImageButton.setOnClickListener(v -> checkPermissionAndOpenPicker());
        
        // Setup logout button
        findViewById(R.id.btnLogout).setOnClickListener(v -> performLogout());
        
        // Setup search users button
        searchUsersButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SearchUsersActivity.class));
        });
        
        // Validate the session token
        Log.d(TAG, "Validating session token...");
        validateSessionManual(token);
        
        // Obtener perfil del usuario
        getUserProfileExample(token);
        
        // Cargar lista de chats
        loadChats();
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
                
                // Verificar si el email está verificado
                if (!response.isEmailVerified()) {
                    Log.d(TAG, "Email not verified, redirecting to verification screen");
                    navigateToVerifyEmail(token);
                    return;
                }
                
                // Email verificado: mostrar contenido normal
                // Mostrar "Hola, {username}"
                String welcomeMessage = "Hola, " + response.getUsername();
                welcomeTextView.setText(welcomeMessage);
                
                Toast.makeText(MainActivity.this, 
                        "Bienvenido, " + response.getUsername() + "!", 
                        Toast.LENGTH_SHORT).show();
                
                // Descargar y mostrar imagen de perfil
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
     * - Si viene desde login → SIEMPRE descargar imagen fresca
     * - Si viene desde sesión guardada:
     *   1. Si cache es válido (< 7 días) → usar cache
     *   2. Si cache expiró → descargar nueva versión
     *   3. Si descarga falla → usar cache antiguo como fallback
     */
    private void downloadProfileImageIfAvailable(UserProfileResponse profile) {
        String imageUrl = profile.getProfileImageUrl();
        
        // Verificar si hay URL de imagen
        if (imageUrl == null || imageUrl.isEmpty()) {
            Log.d(TAG, "No profile image URL available");
            return;
        }
        
        String username = profile.getUsername();
        String fileName = com.example.androidchatproject.utils.ImageDownloader
                .generateProfileImageFileName(username);
        
        // Si viene desde login, SIEMPRE descargar imagen fresca
        if (isComingFromLogin) {
            Log.d(TAG, "🔄 Coming from login - forcing fresh image download for: " + username);
            
            com.example.androidchatproject.utils.ImageDownloader.downloadAndSaveImage(
                this,
                imageUrl,
                fileName,
                new com.example.androidchatproject.utils.ImageDownloader.DownloadCallback() {
                    @Override
                    public void onSuccess(java.io.File imageFile) {
                        Log.d(TAG, "Fresh profile image downloaded after login");
                        Log.d(TAG, "  Path: " + imageFile.getAbsolutePath());
                        Log.d(TAG, "  Size: " + (imageFile.length() / 1024) + " KB");
                        
                        // Cargar la imagen en el ImageView circular
                        loadImageIntoView(imageFile);
                    }

                    @Override
                    public void onError(Exception error) {
                        Log.e(TAG, "Error downloading fresh profile image", error);
                        Toast.makeText(MainActivity.this, 
                                "No se pudo cargar la imagen de perfil", 
                                Toast.LENGTH_SHORT).show();
                    }
                }
            );
            return; // Salir del método, no usar caché
        }
        
        // Si NO viene desde login, usar sistema híbrido con caché
        Log.d(TAG, "Using cache strategy for profile image: " + username);
        
        com.example.androidchatproject.utils.ImageDownloader.loadProfileImageWithCache(
            this,
            imageUrl,
            username,
            new com.example.androidchatproject.utils.ImageDownloader.DownloadCallback() {
                @Override
                public void onSuccess(java.io.File imageFile) {
                    long age = com.example.androidchatproject.utils.ImageDownloader
                            .getImageAgeInDays(MainActivity.this, fileName);
                    
                    if (age == 0) {
                        // Recién descargada
                        Log.d(TAG, "Profile image downloaded and cached");
                        Log.d(TAG, "  Path: " + imageFile.getAbsolutePath());
                        Log.d(TAG, "  Size: " + (imageFile.length() / 1024) + " KB");
                    } else {
                        // Cargada desde cache
                        Log.d(TAG, "Profile image loaded from cache");
                        Log.d(TAG, "  Path: " + imageFile.getAbsolutePath());
                        Log.d(TAG, "  Age: " + age + " days");
                        Log.d(TAG, "  Size: " + (imageFile.length() / 1024) + " KB");
                    }
                    
                    // Cargar la imagen en el ImageView circular
                    loadImageIntoView(imageFile);
                }

                @Override
                public void onError(Exception error) {
                    Log.e(TAG, "Error loading profile image", error);
                    
                    // Solo mostrar toast si es un error real (no cuando hay fallback)
                    if (!com.example.androidchatproject.utils.ImageDownloader
                            .imageExists(MainActivity.this, fileName)) {
                        Toast.makeText(MainActivity.this, 
                                "No se pudo cargar la imagen de perfil", 
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
    }
    
    /**
     * Carga la imagen en el ImageView (ya es circular por el layout)
     */
    private void loadImageIntoView(java.io.File imageFile) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            if (bitmap != null) {
                // El ImageView ya tiene el estilo circular aplicado desde el XML
                profileImageView.setImageBitmap(bitmap);
                Log.d(TAG, "Image displayed in circular ImageView");
            } else {
                Log.e(TAG, "Failed to decode bitmap from file");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading image into view", e);
        }
    }
    
    // ==================== IMAGE UPLOAD ====================
    
    /**
     * Configurar image picker y permisos
     */
    private void setupImagePicker() {
        // Configurar launcher para seleccionar imagen
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        handleImageSelection(imageUri);
                    }
                }
            }
        );
        
        // Configurar launcher para solicitar permisos
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Permiso de galería concedido");
                    openImagePicker();
                } else {
                    Log.w(TAG, "Permiso de galería denegado");
                    showPermissionDeniedDialog();
                }
            }
        );
    }
    
    /**
     * Verificar permisos y abrir selector de imágenes
     */
    private void checkPermissionAndOpenPicker() {
        // Determinar qué permiso usar según versión de Android
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            // Android 12 y anteriores
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        
        // Verificar si ya tiene el permiso
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permiso ya concedido, abriendo selector");
            openImagePicker();
        } else {
            // Verificar si debe mostrar explicación
            if (shouldShowRequestPermissionRationale(permission)) {
                Log.d(TAG, "ℹMostrando explicación de permiso");
                showPermissionRationaleDialog(permission);
            } else {
                Log.d(TAG, "Solicitando permiso de galería");
                requestPermissionLauncher.launch(permission);
            }
        }
    }
    
    /**
     * Mostrar diálogo explicando por qué se necesita el permiso
     */
    private void showPermissionRationaleDialog(String permission) {
        new AlertDialog.Builder(this)
            .setTitle("Permiso necesario")
            .setMessage("Para subir una imagen de perfil, necesitamos acceso a tu galería de fotos.")
            .setPositiveButton("Dar permiso", (dialog, which) -> {
                requestPermissionLauncher.launch(permission);
            })
            .setNegativeButton("Cancelar", (dialog, which) -> {
                Toast.makeText(this, "No se puede subir imagen sin permiso", Toast.LENGTH_SHORT).show();
            })
            .show();
    }
    
    /**
     * Mostrar diálogo cuando el permiso es denegado
     */
    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Permiso denegado")
            .setMessage("Sin acceso a la galería no puedes subir una imagen de perfil. Puedes habilitar el permiso desde la configuración de la aplicación.")
            .setPositiveButton("Entendido", (dialog, which) -> {
                dialog.dismiss();
            })
            .setNegativeButton("Ir a configuración", (dialog, which) -> {
                // Abrir configuración de la app
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            })
            .show();
    }
    
    /**
     * Abrir selector de imágenes
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }
    
    /**
     * Manejar imagen seleccionada
     */
    private void handleImageSelection(Uri imageUri) {
        try {
            // Leer imagen como bytes
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Toast.makeText(this, "Error al leer la imagen", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Convertir a bitmap
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            
            if (bitmap == null) {
                Toast.makeText(this, "Error al decodificar la imagen", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Comprimir imagen si es muy grande
            byte[] imageBytes = compressImage(bitmap);
            
            // Validar tamaño
            if (imageBytes.length > MAX_IMAGE_SIZE) {
                Toast.makeText(this, "La imagen es muy grande (máx 1MB)", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Subir imagen
            uploadProfileImage(imageBytes);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling image selection", e);
            Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Comprimir imagen a JPEG usando ImageHelper
     */
    private byte[] compressImage(Bitmap bitmap) {
        return com.example.androidchatproject.helper.ImageHelper.compressImage(bitmap);
    }
    
    /**
     * Subir imagen de perfil al servidor
     */
    private void uploadProfileImage(byte[] imageBytes) {
        // Mostrar loading
        uploadImageButton.setEnabled(false);
        Toast.makeText(this, "Subiendo imagen...", Toast.LENGTH_SHORT).show();
        
        String fileName = "profile_" + System.currentTimeMillis() + ".jpg";
        
        apiHttpClient.uploadProfileImage(currentToken, imageBytes, fileName, 
            new ApiHttpClientUser.UploadProfileImageCallback() {
                @Override
                public void onSuccess(UploadProfileImageResponse response) {
                    if (response.isValid()) {
                        Log.d(TAG, "✅ Profile image uploaded successfully");
                        Toast.makeText(MainActivity.this, 
                                "Imagen actualizada, descargando...", 
                                Toast.LENGTH_SHORT).show();
                        
                        // Recargar perfil para obtener nueva imagen URL
                        refreshProfileAfterUpload();
                    } else {
                        uploadImageButton.setEnabled(true);
                        Log.e(TAG, "❌ Image upload failed: invalid response");
                        Toast.makeText(MainActivity.this, 
                                "Error: imagen inválida", 
                                Toast.LENGTH_SHORT).show();
                    }
                }
                
                @Override
                public void onError(Exception error) {
                    uploadImageButton.setEnabled(true);
                    Log.e(TAG, "❌ Error uploading profile image", error);
                    // El error ya se muestra en Toast por ErrorHandler
                }
            }
        );
    }
    
    /**
     * Refrescar perfil después de upload exitoso
     * Obtiene nueva URL de imagen y la descarga automáticamente
     */
    private void refreshProfileAfterUpload() {
        Log.d(TAG, "Refreshing profile after image upload...");
        
        apiHttpClient.getUserProfile(currentToken, new ApiHttpClientUser.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfileResponse response) {
                Log.d(TAG, "✅ Profile refreshed, new image URL: " + response.getProfileImageUrl());
                
                String imageUrl = response.getProfileImageUrl();
                String username = response.getUsername();
                
                // Verificar si hay nueva URL de imagen
                if (imageUrl == null || imageUrl.isEmpty()) {
                    uploadImageButton.setEnabled(true);
                    Log.w(TAG, "⚠️ No image URL in refreshed profile");
                    Toast.makeText(MainActivity.this, 
                            "Imagen subida pero URL no disponible", 
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Forzar descarga de la nueva imagen (ignorar caché)
                Log.d(TAG, "Forcing download of new profile image...");
                forceDownloadNewImage(imageUrl, username);
            }
            
            @Override
            public void onError(Exception error) {
                uploadImageButton.setEnabled(true);
                Log.e(TAG, "❌ Error refreshing profile after upload", error);
                Toast.makeText(MainActivity.this, 
                        "Imagen subida pero no se pudo actualizar", 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Forzar descarga de nueva imagen (sin caché)
     */
    private void forceDownloadNewImage(String imageUrl, String username) {
        // Descargar directamente sin usar caché (nueva imagen)
        String fileName = com.example.androidchatproject.utils.ImageDownloader
                .generateProfileImageFileName(username);
        
        com.example.androidchatproject.utils.ImageDownloader.downloadAndSaveImage(
            this,
            imageUrl,
            fileName,
            new com.example.androidchatproject.utils.ImageDownloader.DownloadCallback() {
                @Override
                public void onSuccess(java.io.File imageFile) {
                    uploadImageButton.setEnabled(true);
                    
                    Log.d(TAG, "✅ New profile image downloaded and saved");
                    Log.d(TAG, "  Path: " + imageFile.getAbsolutePath());
                    Log.d(TAG, "  Size: " + (imageFile.length() / 1024) + " KB");
                    
                    Toast.makeText(MainActivity.this, 
                            "¡Imagen actualizada correctamente!", 
                            Toast.LENGTH_SHORT).show();
                    
                    // Mostrar la nueva imagen en el ImageView
                    loadImageIntoView(imageFile);
                }
                
                @Override
                public void onError(Exception error) {
                    uploadImageButton.setEnabled(true);
                    
                    Log.e(TAG, "[ERROR] Error downloading new profile image", error);
                    Toast.makeText(MainActivity.this, 
                            "No se pudo descargar la imagen actualizada", 
                            Toast.LENGTH_SHORT).show();
                }
            }
        );
    }
    
    /**
     * Navegar a pantalla de verificación de email
     */
    private void navigateToVerifyEmail(String token) {
        Intent intent = new Intent(this, VerifyEmailActivity.class);
        intent.putExtra("TOKEN", token);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
     * Cerrar sesión
     */
    private void performLogout() {
        if (currentToken == null || currentToken.isEmpty()) {
            Log.w(TAG, "⚠️ No hay token válido, redirigiendo a login");
            navigateToLogin();
            return;
        }
        
        Log.d(TAG, "🔄 Iniciando logout...");
        Toast.makeText(this, "Cerrando sesión...", Toast.LENGTH_SHORT).show();
        
        apiHttpClient.logout(currentToken, new ApiHttpClientUser.LogoutCallback() {
            @Override
            public void onSuccess(LogoutResponse response) {
                Log.d(TAG, "✅ Logout exitoso en el servidor");
                
                // Limpiar sesión local
                sessionManager.clearSession();
                
                Toast.makeText(MainActivity.this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
                
                // Redirigir al login
                navigateToLogin();
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "❌ Error al cerrar sesión en el servidor", error);
                
                // Aunque falle el API, limpiar sesión local de todos modos
                sessionManager.clearSession();
                
                Toast.makeText(MainActivity.this, "Sesión cerrada localmente", Toast.LENGTH_SHORT).show();
                
                // Redirigir al login
                navigateToLogin();
            }
        });
    }
    
    /**
     * Cargar chats del usuario desde API
     */
    private void loadChats() {
        String token = sessionManager.getToken();
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "No hay token disponible para cargar chats");
            return;
        }
        
        apiHttpClientChats.getAllChats(token, new ApiHttpClientChats.ChatsListCallback() {
            @Override
            public void onSuccess(ChatsListResponse response) {
                List<ChatItem> chats = response.getChats();
                
                if (chats != null && !chats.isEmpty()) {
                    Log.d(TAG, chats.size() + " chats cargados");
                    allChats = chats;
                    chatsAdapter.updateChats(allChats);
                } else {
                    Log.d(TAG, "No hay chats disponibles");
                    allChats = new ArrayList<>();
                    chatsAdapter.updateChats(allChats);
                }
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error al cargar chats: " + error.getMessage());
                Toast.makeText(MainActivity.this, "Error al cargar chats", Toast.LENGTH_SHORT).show();
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
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatsAdapter != null) {
            chatsAdapter.cleanup();
        }
    }
}
