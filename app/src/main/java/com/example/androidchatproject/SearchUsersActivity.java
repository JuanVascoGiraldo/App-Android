package com.example.androidchatproject;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.androidchatproject.adapter.UsersAdapter;
import com.example.androidchatproject.database.ChatsCacheHelper;
import com.example.androidchatproject.database.UsersCacheHelper;
import com.example.androidchatproject.model.chats.ChatItem;
import com.example.androidchatproject.model.user.LogoutResponse;
import com.example.androidchatproject.model.user.UserListItem;
import com.example.androidchatproject.model.user.UsersListResponse;
import com.example.androidchatproject.network.ApiHttpClientUser;
import com.example.androidchatproject.session.SessionManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity para buscar usuarios
 */
public class SearchUsersActivity extends AppCompatActivity {
    
    private static final String TAG = "SearchUsersActivity";
    
    // UI Components
    private TextInputEditText searchEditText;
    private ListView usersListView;
    private ProgressBar progressBar;
    private MaterialCardView offlineCard;
    private View emptyStateLayout;
    
    // Data - Users
    private UsersAdapter adapter;
    private List<UserListItem> allUsers;
    private UsersCacheHelper cacheHelper;
    private ChatsCacheHelper chatsCacheHelper;
    private SessionManager sessionManager;
    private ApiHttpClientUser apiHttpClient;
    private String currentToken;
    private boolean isOfflineMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_users);
        
        // Initialize components
        sessionManager = new SessionManager(this);
        apiHttpClient = new ApiHttpClientUser(this);
        cacheHelper = new UsersCacheHelper(this);
        chatsCacheHelper = new ChatsCacheHelper(this);
        
        // Get token
        currentToken = sessionManager.getToken();
        if (currentToken == null || currentToken.isEmpty()) {
            navigateToLogin();
            return;
        }
        
        initializeViews();
        setupListeners();
        
        // Cargar usuarios
        loadUsers();
    }
    
    private void initializeViews() {
        searchEditText = findViewById(R.id.searchEditText);
        usersListView = findViewById(R.id.usersListView);
        progressBar = findViewById(R.id.progressBar);
        offlineCard = findViewById(R.id.offlineCard);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        
        // Initialize users list
        allUsers = new ArrayList<>();
        adapter = new UsersAdapter(this, allUsers);
        usersListView.setAdapter(adapter);
        
        // Setup logout button
        findViewById(R.id.btnLogout).setOnClickListener(v -> performLogout());
    }
    
    private void setupListeners() {
        // Búsqueda en tiempo real
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Filtrar lista
                adapter.getFilter().filter(s);
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Click en usuario
        usersListView.setOnItemClickListener((parent, view, position, id) -> {
            UserListItem user = adapter.getItem(position);
            handleUserSelected(user);
        });
    }
    
    /**
     * Cargar usuarios (primero intenta desde API, si falla usa caché)
     */
    private void loadUsers() {
        if (isNetworkAvailable()) {
            Log.d(TAG, "Red disponible - Cargando desde API");
            loadUsersFromApi();
        } else {
            Log.w(TAG, "Sin conexión - Cargando desde caché");
            loadUsersFromCache();
        }
    }
    
    /**
     * Cargar usuarios desde API
     */
    private void loadUsersFromApi() {
        setLoading(true);
        isOfflineMode = false;
        offlineCard.setVisibility(View.GONE);
        
        apiHttpClient.getAllUsers(currentToken, new ApiHttpClientUser.UsersListCallback() {
            @Override
            public void onSuccess(UsersListResponse response) {
                setLoading(false);
                
                List<UserListItem> users = response.getUsers();
                
                if (users != null && !users.isEmpty()) {
                    Log.d(TAG, "✅ " + users.size() + " usuarios cargados desde API");
                    
                    // Actualizar lista
                    allUsers = users;
                    adapter.updateUsers(allUsers);
                    
                    // Guardar en caché (sin imágenes)
                    cacheHelper.cacheUsers(users);
                    
                    // Mostrar/ocultar empty state
                    updateEmptyState();
                } else {
                    Log.w(TAG, "⚠️ Lista de usuarios vacía");
                    showEmptyState("No hay usuarios disponibles");
                }
            }
            
            @Override
            public void onError(Exception error) {
                setLoading(false);
                Log.e(TAG, "❌ Error al cargar usuarios desde API", error);
                
                // Intentar cargar desde caché como fallback
                loadUsersFromCache();
            }
        });
    }
    
    /**
     * Cargar usuarios desde caché SQLite
     */
    private void loadUsersFromCache() {
        Log.d(TAG, "📦 Cargando usuarios desde caché...");
        
        if (!cacheHelper.hasCache()) {
            showEmptyState("Sin conexión y sin datos guardados");
            return;
        }
        
        List<UserListItem> cachedUsers = cacheHelper.getCachedUsers();
        
        if (cachedUsers != null && !cachedUsers.isEmpty()) {
            Log.d(TAG, "✅ " + cachedUsers.size() + " usuarios cargados desde caché");
            
            allUsers = cachedUsers;
            adapter.updateUsers(allUsers);
            adapter.setOfflineMode(true);
            isOfflineMode = true;
            
            // Mostrar indicador de offline
            offlineCard.setVisibility(View.VISIBLE);
            
            updateEmptyState();
        } else {
            showEmptyState("Sin conexión y sin datos guardados");
        }
    }
    
    /**
     * Verificar si hay conexión a internet
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
        return false;
    }
    
    /**
     * Mostrar/ocultar loading
     */
    private void setLoading(boolean loading) {
        if (loading) {
            progressBar.setVisibility(View.VISIBLE);
            usersListView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            usersListView.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Actualizar estado vacío
     */
    private void updateEmptyState() {
        if (allUsers.isEmpty()) {
            showEmptyState("No se encontraron usuarios");
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            usersListView.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Mostrar estado vacío
     */
    private void showEmptyState(String message) {
        usersListView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
        // Actualizar mensaje si es necesario
    }
    
    /**
     * Cerrar sesión
     */
    private void performLogout() {
        if (currentToken == null || currentToken.isEmpty()) {
            navigateToLogin();
            return;
        }
        
        Toast.makeText(this, "Cerrando sesión...", Toast.LENGTH_SHORT).show();
        
        apiHttpClient.logout(currentToken, new ApiHttpClientUser.LogoutCallback() {
            @Override
            public void onSuccess(LogoutResponse response) {
                sessionManager.clearSession();
                Toast.makeText(SearchUsersActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
                navigateToLogin();
            }
            
            @Override
            public void onError(Exception error) {
                sessionManager.clearSession();
                Toast.makeText(SearchUsersActivity.this, "Sesión cerrada localmente", Toast.LENGTH_SHORT).show();
                navigateToLogin();
            }
        });
    }
    
    /**
     * Manejar selección de usuario
     * 1. Verificar si ya existe un chat en SQLite
     * 2. Si existe -> Abrir el chat
     * 3. Si no existe -> Abrir diálogo para iniciar conversación
     */
    private void handleUserSelected(UserListItem user) {
        Log.d(TAG, "User selected: " + user.getUsername() + " (ID: " + user.getUserId() + ")");
        
        // Buscar si ya existe un chat con este usuario en SQLite
        ChatItem existingChat = chatsCacheHelper.findChatByUserId(user.getUserId());
        
        if (existingChat != null) {
            // Ya existe un chat, abrir directamente
            Log.d(TAG, "Chat found in cache, opening chat detail");
            openChatDetail(existingChat, user);
        } else {
            // No existe chat, iniciar nueva conversación
            Log.d(TAG, "No chat found, starting new conversation");
            startNewConversation(user);
        }
    }
    
    /**
     * Abrir chat existente
     */
    private void openChatDetail(ChatItem chat, UserListItem user) {
        Intent intent = new Intent(this, ChatDetailActivity.class);
        intent.putExtra("chat_id", chat.getId());
        intent.putExtra("user_id", user.getUserId());
        intent.putExtra("username", user.getUsername());
        intent.putExtra("profile_img", user.getProfileImageUrl());
        startActivity(intent);
    }
    
    /**
     * Iniciar nueva conversación
     * Mostrar diálogo con campo de texto para el primer mensaje
     */
    private void startNewConversation(UserListItem user) {
        // Crear intent para StartConversationActivity
        Intent intent = new Intent(this, StartConversationActivity.class);
        intent.putExtra("user_id", user.getUserId());
        intent.putExtra("username", user.getUsername());
        intent.putExtra("profile_img", user.getProfileImageUrl());
        startActivity(intent);
    }
    
    /**
     * Navegar al login
     */
    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
