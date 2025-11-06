package com.example.androidchatproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.androidchatproject.helper.ProfileImageLoader;
import com.example.androidchatproject.model.chats.CreateChatResponse;
import com.example.androidchatproject.network.ApiHttpClientChats;
import com.example.androidchatproject.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Activity para iniciar una nueva conversación con un usuario
 */
public class StartConversationActivity extends AppCompatActivity {
    
    private static final String TAG = "StartConversation";
    
    // UI Components
    private Toolbar toolbar;
    private ImageView profileImageView;
    private TextView usernameTextView;
    private TextView subtitleTextView;
    private TextInputEditText messageEditText;
    private MaterialButton sendButton;
    private ProgressBar progressBar;
    
    // Data
    private String userId;
    private String username;
    private String profileImageUrl;
    
    // Managers
    private SessionManager sessionManager;
    private ApiHttpClientChats apiHttpClient;
    private ProfileImageLoader imageLoader;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_conversation);
        
        // Initialize managers
        sessionManager = new SessionManager(this);
        apiHttpClient = new ApiHttpClientChats(this);
        imageLoader = new ProfileImageLoader(this);
        
        // Get user data from intent
        userId = getIntent().getStringExtra("user_id");
        username = getIntent().getStringExtra("username");
        profileImageUrl = getIntent().getStringExtra("profile_img");
        
        if (userId == null || username == null) {
            Toast.makeText(this, "Error: Datos de usuario inválidos", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initializeViews();
        setupToolbar();
        setupListeners();
        loadUserProfile();
    }
    
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        profileImageView = findViewById(R.id.profileImageView);
        usernameTextView = findViewById(R.id.usernameTextView);
        subtitleTextView = findViewById(R.id.subtitleTextView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        progressBar = findViewById(R.id.progressBar);
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupListeners() {
        sendButton.setOnClickListener(v -> createChatAndSendMessage());
    }
    
    private void loadUserProfile() {
        usernameTextView.setText(username);
        subtitleTextView.setText("Escribe tu primer mensaje a " + username);
        
        // Load profile image
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            imageLoader.loadProfileImage(profileImageUrl, profileImageView, userId);
        }
    }
    
    /**
     * Crear chat y enviar el primer mensaje
     */
    private void createChatAndSendMessage() {
        String message = messageEditText.getText().toString().trim();
        
        if (message.isEmpty()) {
            Toast.makeText(this, "Escribe un mensaje", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String token = sessionManager.getToken();
        if (token == null) {
            Toast.makeText(this, "Error: No hay sesión activa", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Deshabilitar botón y mostrar loading
        sendButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        sendButton.setVisibility(View.GONE);
        
        Log.d(TAG, "Creating chat with user: " + userId + " and message: " + message);
        
        // Crear el chat con el primer mensaje
        apiHttpClient.createChat(token, userId, message, new ApiHttpClientChats.CreateChatCallback() {
            @Override
            public void onSuccess(CreateChatResponse response) {
                Log.d(TAG, "Chat created successfully with ID: " + response.getChatId());
                
                runOnUiThread(() -> {
                    Toast.makeText(StartConversationActivity.this, 
                            "Conversación iniciada", Toast.LENGTH_SHORT).show();
                    
                    // Abrir el chat directamente con el chat_id recibido
                    openChatDetail(response.getChatId());
                });
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error creating chat", error);
                
                runOnUiThread(() -> {
                    sendButton.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    sendButton.setVisibility(View.VISIBLE);
                    
                    Toast.makeText(StartConversationActivity.this, 
                            "Error al crear la conversación", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * Abrir la vista de chat con el ID del chat creado
     */
    private void openChatDetail(String chatId) {
        Intent intent = new Intent(this, ChatDetailActivity.class);
        intent.putExtra("chat_id", chatId);
        intent.putExtra("user_id", userId);
        intent.putExtra("username", username);
        intent.putExtra("profile_img", profileImageUrl);
        
        // Limpiar stack y abrir MainActivity primero, luego el chat
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainIntent);
        
        // Luego abrir el chat
        startActivity(intent);
        finish();
    }
    
    /**
     * Navegar al MainActivity (pantalla principal con lista de chats)
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
