package com.example.androidchatproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.androidchatproject.model.user.AuthRequest;
import com.example.androidchatproject.model.user.AuthResponse;
import com.example.androidchatproject.model.user.RegisterRequest;
import com.example.androidchatproject.model.user.RegisterResponse;
import com.example.androidchatproject.network.ApiHttpClientUser;
import com.example.androidchatproject.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Pattern;

/**
 * Activity para registro de nuevos usuarios
 */
public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    
    // Patrón para validar contraseña: al menos 8 caracteres, una mayúscula, una minúscula, un número
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$"
    );

    // Views
    private TextInputLayout usernameInputLayout;
    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputLayout confirmPasswordInputLayout;
    
    private TextInputEditText usernameEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText confirmPasswordEditText;
    
    private MaterialButton registerButton;
    private TextView loginLinkTextView;
    private View loadingOverlay;

    // API y Session
    private ApiHttpClientUser apiHttpClient;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Inicializar API y Session
        apiHttpClient = new ApiHttpClientUser(this);
        sessionManager = new SessionManager(this);

        // Inicializar views
        initViews();
        setupListeners();
    }

    private void initViews() {
        usernameInputLayout = findViewById(R.id.usernameInputLayout);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);
        
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        
        registerButton = findViewById(R.id.registerButton);
        loginLinkTextView = findViewById(R.id.loginLinkTextView);
        loadingOverlay = findViewById(R.id.loadingOverlay);
    }

    private void setupListeners() {
        registerButton.setOnClickListener(v -> attemptRegister());
        
        loginLinkTextView.setOnClickListener(v -> {
            // Navegar a Login
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void attemptRegister() {
        // Limpiar errores previos
        usernameInputLayout.setError(null);
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);

        // Obtener valores
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        // Validar campos
        if (!validateInput(username, email, password, confirmPassword)) {
            return;
        }

        // Mostrar loading
        setLoading(true);

        // Crear request
        RegisterRequest request = new RegisterRequest(username, email, password, confirmPassword);

        // Llamar a la API
        apiHttpClient.registerUser(request, new ApiHttpClientUser.RegisterCallback() {
            @Override
            public void onSuccess(RegisterResponse response) {
                setLoading(false);
                
                if (response.isSuccess()) {
                    Toast.makeText(RegisterActivity.this, 
                            R.string.register_success, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Registration successful");
                    
                    // Hacer login automático con las credenciales
                    performAutoLogin(email, password);
                }
            }

            @Override
            public void onError(Exception error) {
                setLoading(false);
                // El error ya se muestra automáticamente por ErrorHandler
                Log.e(TAG, "Registration error", error);
                
                // Limpiar contraseñas por seguridad
                passwordEditText.setText("");
                confirmPasswordEditText.setText("");
            }
        });
    }

    private boolean validateInput(String username, String email, String password, String confirmPassword) {
        boolean isValid = true;

        // Validar username
        if (username.isEmpty()) {
            usernameInputLayout.setError(getString(R.string.error_empty_username));
            isValid = false;
        } else if (username.length() < 3) {
            usernameInputLayout.setError(getString(R.string.error_short_username));
            isValid = false;
        }

        // Validar email
        if (email.isEmpty()) {
            emailInputLayout.setError(getString(R.string.error_empty_email));
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError(getString(R.string.error_invalid_email));
            isValid = false;
        }

        // Validar password
        if (password.isEmpty()) {
            passwordInputLayout.setError(getString(R.string.error_empty_password));
            isValid = false;
        } else if (password.length() < 8) {
            passwordInputLayout.setError(getString(R.string.error_short_password));
            isValid = false;
        } else if (!PASSWORD_PATTERN.matcher(password).matches()) {
            passwordInputLayout.setError(getString(R.string.error_invalid_password_format));
            isValid = false;
        }

        // Validar confirm password
        if (confirmPassword.isEmpty()) {
            confirmPasswordInputLayout.setError(getString(R.string.error_empty_confirm_password));
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordInputLayout.setError(getString(R.string.error_passwords_dont_match));
            isValid = false;
        }

        return isValid;
    }

    /**
     * Realiza login automático después del registro exitoso
     */
    private void performAutoLogin(String email, String password) {
        Log.d(TAG, "Performing auto-login...");
        
        // Crear request de login (sin remember me para auto-login)
        AuthRequest loginRequest = new AuthRequest(email, password, false);
        
        apiHttpClient.login(loginRequest, new ApiHttpClientUser.LoginCallback() {
            @Override
            public void onSuccess(AuthResponse response) {
                String token = response.getJwt();
                String expirationDate = response.getExpirationDate();
                
                // Guardar token en memoria (no persistente)
                sessionManager.saveToken(token, expirationDate, false);
                
                Toast.makeText(RegisterActivity.this, 
                        R.string.login_success, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Auto-login successful");
                
                // Navegar a MainActivity pasando el token por Intent
                navigateToMain(token, expirationDate);
            }

            @Override
            public void onError(Exception error) {
                // Si falla el auto-login, navegar a Login manual
                Log.e(TAG, "Auto-login failed", error);
                Toast.makeText(RegisterActivity.this, 
                        "Registro exitoso. Por favor inicia sesión.", Toast.LENGTH_LONG).show();
                navigateToLogin();
            }
        });
    }

    private void setLoading(boolean loading) {
        loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!loading);
        usernameEditText.setEnabled(!loading);
        emailEditText.setEnabled(!loading);
        passwordEditText.setEnabled(!loading);
        confirmPasswordEditText.setEnabled(!loading);
    }

    private void navigateToMain(String token, String expirationDate) {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.putExtra("TOKEN", token);
        intent.putExtra("EXPIRATION_DATE", expirationDate);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
