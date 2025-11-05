package com.example.androidchatproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.androidchatproject.helper.ValidationHelper;
import com.example.androidchatproject.model.user.SendResetPasswordRequest;
import com.example.androidchatproject.model.user.SendResetPasswordResponse;
import com.example.androidchatproject.network.ApiHttpClientUser;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Activity para solicitar recuperación de contraseña
 */
public class ForgotPasswordActivity extends AppCompatActivity {
    
    private static final String TAG = "ForgotPasswordActivity";
    
    // UI Components
    private TextInputEditText emailEditText;
    private MaterialButton sendButton;
    private MaterialButton backToLoginButton;
    private ProgressBar progressBar;
    
    // API Client
    private ApiHttpClientUser apiHttpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        
        // Initialize components
        apiHttpClient = new ApiHttpClientUser(this);
        
        initializeViews();
        setupButtons();
    }
    
    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        sendButton = findViewById(R.id.sendButton);
        backToLoginButton = findViewById(R.id.backToLoginButton);
        progressBar = findViewById(R.id.progressBar);
    }
    
    private void setupButtons() {
        sendButton.setOnClickListener(v -> sendResetPasswordEmail());
        backToLoginButton.setOnClickListener(v -> finish());
    }
    
    /**
     * Enviar email para recuperar contraseña
     */
    private void sendResetPasswordEmail() {
        String email = emailEditText.getText().toString().trim();
        
        // Validar email
        if (email.isEmpty()) {
            emailEditText.setError("Ingresa tu correo electrónico");
            emailEditText.requestFocus();
            return;
        }
        
        if (!ValidationHelper.isValidEmail(email)) {
            emailEditText.setError("Correo electrónico inválido");
            emailEditText.requestFocus();
            return;
        }
        
        // Mostrar loading
        setLoading(true);
        
        Log.d(TAG, "Enviando solicitud de reset de contraseña para: " + email);
        
        SendResetPasswordRequest request = new SendResetPasswordRequest(email);
        
        apiHttpClient.sendResetPasswordEmail(request, new ApiHttpClientUser.SendResetPasswordCallback() {
            @Override
            public void onSuccess(SendResetPasswordResponse response) {
                setLoading(false);
                
                Log.d(TAG, "[SUCCESS] Email de recuperación enviado exitosamente");
                Log.d(TAG, "  Recover ID: " + response.getId());
                Log.d(TAG, "  Token: " + response.getToken());
                Log.d(TAG, "  Valid until: " + response.getValidUntil());
                
                Toast.makeText(ForgotPasswordActivity.this, 
                    "Código enviado a tu correo", Toast.LENGTH_LONG).show();
                
                // Navegar a la pantalla de confirmación
                navigateToResetPassword(response);
            }
            
            @Override
            public void onError(Exception error) {
                setLoading(false);
                Log.e(TAG, "[ERROR] Error al enviar email de recuperación", error);
                // El error ya se muestra automáticamente por ErrorHandler
            }
        });
    }
    
    /**
     * Navegar a ResetPasswordActivity con los datos
     */
    private void navigateToResetPassword(SendResetPasswordResponse response) {
        Intent intent = new Intent(this, ResetPasswordActivity.class);
        intent.putExtra("RECOVER_ID", response.getId());
        intent.putExtra("RECOVER_TOKEN", response.getToken());
        intent.putExtra("VALID_UNTIL", response.getValidUntil());
        intent.putExtra("EMAIL", emailEditText.getText().toString().trim());
        startActivity(intent);
        finish();
    }
    
    /**
     * Mostrar/ocultar loading
     */
    private void setLoading(boolean loading) {
        if (loading) {
            progressBar.setVisibility(View.VISIBLE);
            sendButton.setEnabled(false);
            sendButton.setText("");
            emailEditText.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            sendButton.setEnabled(true);
            sendButton.setText("Enviar código");
            emailEditText.setEnabled(true);
        }
    }
}
