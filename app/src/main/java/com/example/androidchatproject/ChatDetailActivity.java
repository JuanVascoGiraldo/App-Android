package com.example.androidchatproject;

import android.app.DownloadManager;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.androidchatproject.adapter.MessagesAdapter;
import com.example.androidchatproject.database.ChatsCacheHelper;
import com.example.androidchatproject.model.chats.ChatDetailResponse;
import com.example.androidchatproject.model.chats.MessageResponse;
import com.example.androidchatproject.model.chats.SendMessageResponse;
import com.example.androidchatproject.network.ApiHttpClientChats;
import com.example.androidchatproject.session.SessionManager;
import com.example.androidchatproject.helper.ProfileImageLoader;
import com.example.androidchatproject.helpers.NotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

public class ChatDetailActivity extends AppCompatActivity {
    
    private static final String TAG = "ChatDetailActivity";
    private static final int MAX_FILE_SIZE = 40 * 1024 * 1024; // 40 MB
    
    // UI Components
    private MaterialToolbar toolbar;
    private ImageView profileImageView;
    private TextView usernameTextView;
    private MaterialCardView offlineCard;
    private ListView messagesListView;
    private EditText messageEditText;
    private ImageButton attachButton;
    private FloatingActionButton sendButton;
    private LinearLayout attachmentPreviewContainer;
    private ImageView attachmentPreviewImage;
    private TextView attachmentNameText;
    private ImageButton removeAttachmentButton;
    
    // Data
    private String chatId;
    private String otherUserId;
    private String otherUsername;
    private String otherUserProfileImg;
    private String currentUserId;
    
    // Managers
    private SessionManager sessionManager;
    private ApiHttpClientChats apiHttpClient;
    private ChatsCacheHelper chatsCacheHelper;
    private ProfileImageLoader imageLoader;
    private MessagesAdapter messagesAdapter;
    private NotificationHelper notificationHelper;
    
    // Attachment handling
    private byte[] selectedAttachmentBytes = null;
    private String selectedAttachmentName = null;
    private String selectedAttachmentMimeType = null;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    
    // Offline mode
    private boolean isOfflineMode = false;
    
    // Auto-refresh messages
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private static final long REFRESH_INTERVAL = 5000; // 5 segundos
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);
        
        // Inicializar managers
        sessionManager = new SessionManager(this);
        notificationHelper = new NotificationHelper(this);
        apiHttpClient = new ApiHttpClientChats(this);
        chatsCacheHelper = new ChatsCacheHelper(this);
        imageLoader = new ProfileImageLoader(this);
        
        // Obtener currentUserId del SessionManager
        currentUserId = sessionManager.getUserId();
        Log.d(TAG, "Current userId from SessionManager: " + currentUserId);
        if (currentUserId == null) {
            Toast.makeText(this, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "UserId is null - user needs to login again");
            finish();
            return;
        }
        
        // Obtener datos del Intent
        Intent intent = getIntent();
        chatId = intent.getStringExtra("chat_id");
        otherUserId = intent.getStringExtra("user_id");
        otherUsername = intent.getStringExtra("username");
        otherUserProfileImg = intent.getStringExtra("profile_img");
        
        if (chatId == null || otherUserId == null) {
            Toast.makeText(this, "Error: Datos del chat inválidos", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Inicializar UI
        initializeViews();
        setupToolbar();
        setupAdapter();
        setupFilePickerLauncher();
        
        // Verificar conexión y cargar mensajes
        boolean networkAvailable = isNetworkAvailable();
        Log.d(TAG, "Network available: " + networkAvailable);
        
        if (networkAvailable) {
            isOfflineMode = false;
            offlineCard.setVisibility(View.GONE);
            Log.d(TAG, "Online mode - loading messages from API");
            loadMessages();
            
            // Iniciar auto-refresh de mensajes cada 30 segundos
            startAutoRefresh();
        } else {
            isOfflineMode = true;
            offlineCard.setVisibility(View.VISIBLE);
            Log.d(TAG, "Offline mode - loading messages from cache");
            loadMessagesFromCache();
        }
        
        // Setup listeners
        setupListeners();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reiniciar auto-refresh si está en modo online
        if (!isOfflineMode && refreshHandler == null) {
            startAutoRefresh();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Detener auto-refresh para ahorrar recursos
        stopAutoRefresh();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Asegurarse de detener auto-refresh
        stopAutoRefresh();
    }
    
    /**
     * Iniciar refresco automático de mensajes
     */
    private void startAutoRefresh() {
        if (refreshHandler == null) {
            refreshHandler = new Handler(Looper.getMainLooper());
        }
        
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isOfflineMode) {
                    Log.d(TAG, "Auto-refreshing messages...");
                    loadMessages(false); // No hacer scroll automático en refresh
                }
                
                // Programar siguiente ejecución
                if (refreshHandler != null) {
                    refreshHandler.postDelayed(this, REFRESH_INTERVAL);
                }
            }
        };
        
        // Iniciar refresco después de 30 segundos
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
        Log.d(TAG, "Auto-refresh started (every 30 seconds)");
    }
    
    /**
     * Detener refresco automático
     */
    private void stopAutoRefresh() {
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
            refreshHandler = null;
            refreshRunnable = null;
            Log.d(TAG, "Auto-refresh stopped");
        }
    }
    
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        profileImageView = findViewById(R.id.profileImageView);
        usernameTextView = findViewById(R.id.usernameTextView);
        offlineCard = findViewById(R.id.offlineCard);
        messagesListView = findViewById(R.id.messagesListView);
        messageEditText = findViewById(R.id.messageEditText);
        attachButton = findViewById(R.id.attachButton);
        sendButton = findViewById(R.id.sendButton);
        attachmentPreviewContainer = findViewById(R.id.attachmentPreviewContainer);
        attachmentPreviewImage = findViewById(R.id.attachmentPreviewImage);
        attachmentNameText = findViewById(R.id.attachmentNameText);
        removeAttachmentButton = findViewById(R.id.removeAttachmentButton);
        
        // Mostrar información del otro usuario
        usernameTextView.setText(otherUsername != null ? otherUsername : "Usuario");
        if (otherUserProfileImg != null && !otherUserProfileImg.isEmpty()) {
            imageLoader.loadProfileImage(otherUserProfileImg, profileImageView, otherUserId);
        }
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupAdapter() {
        messagesAdapter = new MessagesAdapter(this, currentUserId);
        messagesListView.setAdapter(messagesAdapter);
        
        // Configurar listener para clicks en attachments
        messagesAdapter.setOnAttachmentClickListener((url, fileName, mimeType) -> {
            handleAttachmentClick(url, fileName, mimeType);
        });
    }
    
    private void setupFilePickerLauncher() {
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri fileUri = result.getData().getData();
                    handleSelectedFile(fileUri);
                }
            }
        );
    }
    
    private void setupListeners() {
        attachButton.setOnClickListener(v -> openFilePicker());
        removeAttachmentButton.setOnClickListener(v -> clearAttachment());
        sendButton.setOnClickListener(v -> sendMessage());
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        
        return false;
    }
    
    private void loadMessages() {
        loadMessages(true);
    }
    
    private void loadMessages(boolean scrollToEnd) {
        String token = sessionManager.getToken();
        Log.d(TAG, "loadMessages called - chatId: " + chatId + ", token: " + (token != null ? "exists" : "null"));
        
        if (token == null) {
            Toast.makeText(this, "Error: No hay sesión activa", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        Log.d(TAG, "Making API call to getChatById with chatId: " + chatId);
        apiHttpClient.getChatById(token, chatId, new ApiHttpClientChats.ChatDetailCallback() {
            @Override
            public void onSuccess(ChatDetailResponse response) {
                Log.d(TAG, "API SUCCESS - Messages loaded: " + response.getMessages().size());
                
                // Guardar posición actual del scroll
                int currentPosition = messagesListView.getFirstVisiblePosition();
                
                messagesAdapter.setMessages(response.getMessages());
                
                // Solo hacer scroll al final si es el primer load o se envió un mensaje
                if (scrollToEnd) {
                    scrollToBottom();
                } else {
                    // Mantener posición actual (para auto-refresh)
                    messagesListView.setSelection(currentPosition);
                }
                
                // Guardar en caché
                chatsCacheHelper.cacheChatDetail(response);
                Log.d(TAG, "Messages cached successfully");
                
                // Actualizar el último mensaje visto para este chat
                // Esto evitará que se muestren notificaciones para mensajes ya vistos
                if (response.getMessages() != null && !response.getMessages().isEmpty()) {
                    List<MessageResponse> messages = response.getMessages();
                    MessageResponse lastMessage = messages.get(messages.size() - 1);
                    notificationHelper.saveLastSeenMessageId(chatId, lastMessage.getId());
                    Log.d(TAG, "Saved last seen message ID: " + lastMessage.getId());
                }
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "API ERROR loading messages: " + error.getMessage(), error);
                // Intentar cargar desde caché
                loadMessagesFromCache();
            }
        });
    }
    
    private void loadMessagesFromCache() {
        ChatDetailResponse cachedChat = chatsCacheHelper.getCachedChatDetail(chatId);
        if (cachedChat != null && cachedChat.getMessages() != null) {
            messagesAdapter.setMessages(cachedChat.getMessages());
            scrollToBottom();
            Log.d(TAG, "Messages loaded from cache: " + cachedChat.getMessages().size());
        } else {
            Log.d(TAG, "No cached messages found");
            Toast.makeText(this, "No hay mensajes guardados", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openFilePicker() {
        if (isOfflineMode) {
            Toast.makeText(this, "No puedes adjuntar archivos en modo offline", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"image/*", "application/pdf", "audio/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        filePickerLauncher.launch(intent);
    }
    
    private void handleSelectedFile(Uri fileUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                Toast.makeText(this, "Error al leer el archivo", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Leer bytes del archivo
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[1024];
            int nRead;
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            inputStream.close();
            
            selectedAttachmentBytes = buffer.toByteArray();
            
            // Verificar tamaño
            if (selectedAttachmentBytes.length > MAX_FILE_SIZE) {
                Toast.makeText(this, "El archivo es demasiado grande (máx 10MB)", Toast.LENGTH_SHORT).show();
                clearAttachment();
                return;
            }
            
            // Obtener nombre del archivo
            String fileName = getFileName(fileUri);
            selectedAttachmentName = fileName;
            
            // Obtener MIME type real del sistema
            selectedAttachmentMimeType = getContentResolver().getType(fileUri);
            if (selectedAttachmentMimeType == null || selectedAttachmentMimeType.isEmpty()) {
                // Fallback: determinar por extensión
                selectedAttachmentMimeType = getMimeTypeFromFileName(fileName);
            }
            
            Log.d(TAG, "File selected - Name: " + fileName + ", MimeType: " + selectedAttachmentMimeType + ", Size: " + selectedAttachmentBytes.length + " bytes");
            
            // Mostrar preview
            showAttachmentPreview(fileName);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling file", e);
            Toast.makeText(this, "Error al procesar el archivo", Toast.LENGTH_SHORT).show();
        }
    }
    
    private String getFileName(Uri uri) {
        String fileName = "archivo";
        String path = uri.getPath();
        if (path != null) {
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash != -1) {
                fileName = path.substring(lastSlash + 1);
            }
        }
        return fileName;
    }
    
    private void showAttachmentPreview(String fileName) {
        attachmentPreviewContainer.setVisibility(View.VISIBLE);
        attachmentNameText.setText(fileName);
        
        // Mostrar icono según tipo de archivo
        if (fileName.toLowerCase().endsWith(".jpg") || 
            fileName.toLowerCase().endsWith(".jpeg") || 
            fileName.toLowerCase().endsWith(".png")) {
            attachmentPreviewImage.setImageResource(R.drawable.ic_image_placeholder);
        } else if (fileName.toLowerCase().endsWith(".pdf")) {
            attachmentPreviewImage.setImageResource(R.drawable.ic_pdf);
        } else if (fileName.toLowerCase().endsWith(".mp3") || 
                   fileName.toLowerCase().endsWith(".wav")) {
            attachmentPreviewImage.setImageResource(R.drawable.ic_audio);
        } else {
            attachmentPreviewImage.setImageResource(R.drawable.ic_file);
        }
    }
    
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
    
    private void clearAttachment() {
        selectedAttachmentBytes = null;
        selectedAttachmentName = null;
        selectedAttachmentMimeType = null;
        attachmentPreviewContainer.setVisibility(View.GONE);
    }
    
    private void sendMessage() {
        if (isOfflineMode) {
            Toast.makeText(this, "No puedes enviar mensajes en modo offline", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String content = messageEditText.getText().toString().trim();
        
        // Validar que haya contenido o attachment
        if (content.isEmpty() && selectedAttachmentBytes == null) {
            Toast.makeText(this, "Escribe un mensaje o adjunta un archivo", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String token = sessionManager.getToken();
        if (token == null) {
            Toast.makeText(this, "Error: No hay sesión activa", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Deshabilitar botón de envío
        sendButton.setEnabled(false);
        
        if (selectedAttachmentBytes != null) {
            // Enviar con attachment
            apiHttpClient.sendMessageWithAttachment(
                token,
                chatId,
                content.isEmpty() ? null : content,
                selectedAttachmentBytes,
                selectedAttachmentName,
                selectedAttachmentMimeType,
                new ApiHttpClientChats.SendMessageCallback() {
                    @Override
                    public void onSuccess(SendMessageResponse response) {
                        Log.d(TAG, "Message sent successfully");
                        onMessageSentSuccess();
                    }
                    
                    @Override
                    public void onError(Exception error) {
                        Log.e(TAG, "Error sending message", error);
                        sendButton.setEnabled(true);
                    }
                }
            );
        } else {
            // Enviar solo texto
            apiHttpClient.sendMessage(
                token,
                chatId,
                content,
                new ApiHttpClientChats.SendMessageCallback() {
                    @Override
                    public void onSuccess(SendMessageResponse response) {
                        Log.d(TAG, "Message sent successfully");
                        onMessageSentSuccess();
                    }
                    
                    @Override
                    public void onError(Exception error) {
                        Log.e(TAG, "Error sending message", error);
                        sendButton.setEnabled(true);
                    }
                }
            );
        }
    }
    
    private void onMessageSentSuccess() {
        // Limpiar campos
        messageEditText.setText("");
        clearAttachment();
        sendButton.setEnabled(true);
        
        // Recargar mensajes después de un pequeño delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            loadMessages();
        }, 500);
    }
    
    private void scrollToBottom() {
        messagesListView.post(() -> {
            messagesListView.setSelection(messagesAdapter.getCount() - 1);
        });
    }
    
    private void handleAttachmentClick(String url, String fileName, String mimeType) {
        Log.d(TAG, "Attachment clicked - URL: " + url + ", MimeType: " + mimeType);
        
        // Si estamos offline, mostrar mensaje
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Sin conexión. No se puede descargar el archivo.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Determinar acción según mime type
        if (mimeType != null && mimeType.startsWith("image/")) {
            // Para imágenes, abrir en navegador o visor de imágenes
            openUrlInBrowser(url);
        } else {
            // Para otros archivos, descargar
            downloadFile(url, fileName);
        }
    }
    
    private void openUrlInBrowser(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening URL", e);
            Toast.makeText(this, "No se puede abrir el archivo", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void downloadFile(String url, String fileName) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle("Descargando " + fileName);
            request.setDescription("Descargando archivo adjunto");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                downloadManager.enqueue(request);
                Toast.makeText(this, "Descargando " + fileName, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error downloading file", e);
            Toast.makeText(this, "Error al descargar el archivo", Toast.LENGTH_SHORT).show();
        }
    }
}
