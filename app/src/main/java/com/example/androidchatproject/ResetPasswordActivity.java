package com.example.androidchatproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.androidchatproject.helper.ValidationHelper;
import com.example.androidchatproject.model.user.ResetPasswordConfirmRequest;
import com.example.androidchatproject.model.user.ResetPasswordConfirmResponse;
import com.example.androidchatproject.network.ApiHttpClientUser;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Activity para confirmar el reset de contraseña con código
 */
public class ResetPasswordActivity extends AppCompatActivity {
    
    private static final String TAG = "ResetPasswordActivity";
    
    // UI Components
    private TextView emailInfoTextView;
    private TextInputEditText codeEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText confirmPasswordEditText;
    private MaterialButton resetButton;
    private MaterialButton backToLoginButton;
    private ProgressBar progressBar;
    
    // API Client
    private ApiHttpClientUser apiHttpClient;
    
    // Data from Intent
    private String recoverId;
    private String recoverToken;
    private String validUntil;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);
        
        // Initialize components
        apiHttpClient = new ApiHttpClientUser(this);
        
        // Get data from Intent
        Intent intent = getIntent();
        recoverId = intent.getStringExtra("RECOVER_ID");
        recoverToken = intent.getStringExtra("RECOVER_TOKEN");
        validUntil = intent.getStringExtra("VALID_UNTIL");
        email = intent.getStringExtra("EMAIL");
        
        // Validar que tenemos los datos necesarios
        if (recoverId == null || recoverToken == null) {
            Log.e(TAG, "Missing recovery data");
            Toast.makeText(this, "Error: Datos de recuperación no válidos", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }
        
        Log.d(TAG, "Recovery ID: " + recoverId);
        Log.d(TAG, "Recovery Token: " + recoverToken);
        
        initializeViews();
        setupButtons();
    }
    
    private void initializeViews() {
        emailInfoTextView = findViewById(R.id.emailInfoTextView);
        codeEditText = findViewById(R.id.codeEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        resetButton = findViewById(R.id.resetButton);
        backToLoginButton = findViewById(R.id.backToLoginButton);
        progressBar = findViewById(R.id.progressBar);
        
        // Mostrar email
        if (email != null && !email.isEmpty()) {
            emailInfoTextView.setText("Enviado a: " + email);
        }
    }
    
    private void setupButtons() {
        resetButton.setOnClickListener(v -> confirmResetPassword());
        backToLoginButton.setOnClickListener(v -> navigateToLogin());
    }
    
    /**
     * Confirmar reset de contraseña
     */
    private void confirmResetPassword() {
        String code = codeEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        
        // Validar código
        if (code.isEmpty()) {
            codeEditText.setError("Ingresa el código de verificación");
            codeEditText.requestFocus();
            return;
        }
        
        if (!ValidationHelper.isValidVerificationCode(code)) {
            codeEditText.setError("El código debe tener 6 dígitos");
            codeEditText.requestFocus();
            return;
        }
        
        // Validar contraseña
        if (password.isEmpty()) {
            passwordEditText.setError("Ingresa tu nueva contraseña");
            passwordEditText.requestFocus();
            return;
        }
        
        if (!ValidationHelper.isValidPassword(password)) {
            passwordEditText.setError("Mínimo 8 caracteres, una mayúscula, una minúscula y un número");
            passwordEditText.requestFocus();
            return;
        }
        
        // Validar confirmación
        if (confirmPassword.isEmpty()) {
            confirmPasswordEditText.setError("Confirma tu contraseña");
            confirmPasswordEditText.requestFocus();
            return;
        }
        
        if (!ValidationHelper.passwordsMatch(password, confirmPassword)) {
            confirmPasswordEditText.setError("Las contraseñas no coinciden");
            confirmPasswordEditText.requestFocus();
            return;
        }
        
        // Mostrar loading
        setLoading(true);
        
        Log.d(TAG, "Confirmando reset de contraseña");
        
        ResetPasswordConfirmRequest request = new ResetPasswordConfirmRequest(
            recoverId,
            recoverToken,
            code,
            password,
            confirmPassword
        );
        
        apiHttpClient.confirmResetPassword(request, new ApiHttpClientUser.ResetPasswordConfirmCallback() {
            @Override
            public void onSuccess(ResetPasswordConfirmResponse response) {
                setLoading(false);
                
                Log.d(TAG, "[SUCCESS] Contraseña restablecida exitosamente");
                Log.d(TAG, "  Success: " + response.isSuccess());
                
                Toast.makeText(ResetPasswordActivity.this, 
                    "¡Contraseña restablecida exitosamente!", Toast.LENGTH_LONG).show();
                
                // Navegar al login
                navigateToLogin();
            }
            
            @Override
            public void onError(Exception error) {
                setLoading(false);
                Log.e(TAG, "[ERROR] Error al restablecer contraseña", error);
                // El error ya se muestra automáticamente por ErrorHandler
            }
        });
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
    
    /**
     * Mostrar/ocultar loading
     */
    private void setLoading(boolean loading) {
        if (loading) {
            progressBar.setVisibility(View.VISIBLE);
            resetButton.setEnabled(false);
            resetButton.setText("");
            codeEditText.setEnabled(false);
            passwordEditText.setEnabled(false);
            confirmPasswordEditText.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            resetButton.setEnabled(true);
            resetButton.setText("Restablecer contraseña");
            codeEditText.setEnabled(true);
            passwordEditText.setEnabled(true);
            confirmPasswordEditText.setEnabled(true);
        }
    }
}
