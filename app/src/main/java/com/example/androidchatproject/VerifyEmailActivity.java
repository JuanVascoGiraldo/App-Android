package com.example.androidchatproject;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.androidchatproject.model.user.*;
import com.example.androidchatproject.network.ApiHttpClientUser;
import com.example.androidchatproject.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Activity para verificar email con código de 6 dígitos
 */
public class VerifyEmailActivity extends AppCompatActivity {
    
    private static final String TAG = "VerifyEmailActivity";
    private static final long COUNTDOWN_TIME = 60000; // 1 minuto en milisegundos
    
    // UI Components
    private TextInputEditText[] codeDigits;
    private MaterialButton verifyButton;
    private MaterialButton resendButton;
    private ProgressBar progressBar;
    
    // API & Session
    private ApiHttpClientUser apiHttpClient;
    private SessionManager sessionManager;
    private String token;
    
    // Countdown timer
    private CountDownTimer countDownTimer;
    private boolean canResend = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);
        
        // Initialize components
        sessionManager = new SessionManager(this);
        apiHttpClient = new ApiHttpClientUser(this);
        
        // Get token from Intent or SessionManager
        token = getIntent().getStringExtra("TOKEN");
        if (token == null || token.isEmpty()) {
            token = sessionManager.getToken();
        }
        
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "No token available");
            navigateToLogin();
            return;
        }
        
        initializeViews();
        setupCodeInputs();
        setupButtons();
        
        // Iniciar countdown automáticamente (ya se envió un código)
        startCountdown();
    }
    
    private void initializeViews() {
        // Inicializar las 6 casillas de código
        codeDigits = new TextInputEditText[6];
        codeDigits[0] = findViewById(R.id.codeDigit1);
        codeDigits[1] = findViewById(R.id.codeDigit2);
        codeDigits[2] = findViewById(R.id.codeDigit3);
        codeDigits[3] = findViewById(R.id.codeDigit4);
        codeDigits[4] = findViewById(R.id.codeDigit5);
        codeDigits[5] = findViewById(R.id.codeDigit6);
        
        verifyButton = findViewById(R.id.verifyButton);
        resendButton = findViewById(R.id.resendButton);
        progressBar = findViewById(R.id.progressBar);
        
        // Setup logout button
        findViewById(R.id.btnLogout).setOnClickListener(v -> performLogout());
    }
    
    /**
     * Configurar auto-avance entre casillas
     */
    private void setupCodeInputs() {
        for (int i = 0; i < codeDigits.length; i++) {
            final int index = i;
            
            // Auto-avanzar al siguiente campo
            codeDigits[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < codeDigits.length - 1) {
                        // Mover al siguiente campo
                        codeDigits[index + 1].requestFocus();
                    }
                }
                
                @Override
                public void afterTextChanged(Editable s) {}
            });
            
            // Retroceder con backspace
            codeDigits[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (codeDigits[index].getText().toString().isEmpty() && index > 0) {
                        // Mover al campo anterior
                        codeDigits[index - 1].requestFocus();
                        codeDigits[index - 1].setText("");
                    }
                }
                return false;
            });
        }
        
        // Focus en el primer campo
        codeDigits[0].requestFocus();
    }
    
    private void setupButtons() {
        verifyButton.setOnClickListener(v -> attemptVerification());
        resendButton.setOnClickListener(v -> resendCode());
    }
    
    /**
     * Obtener el código completo de las 6 casillas
     */
    private String getVerificationCode() {
        StringBuilder code = new StringBuilder();
        for (TextInputEditText digit : codeDigits) {
            code.append(digit.getText().toString());
        }
        return code.toString();
    }
    
    /**
     * Validar y enviar código de verificación
     */
    private void attemptVerification() {
        String code = getVerificationCode();
        
        // Validar que tenga 6 dígitos
        if (code.length() != 6) {
            Toast.makeText(this, "Por favor ingresa los 6 dígitos", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validar que sean solo números
        if (!code.matches("\\d{6}")) {
            Toast.makeText(this, "El código debe contener solo números", Toast.LENGTH_SHORT).show();
            return;
        }
        
        setLoading(true);
        
        VerifyEmailRequest request = new VerifyEmailRequest(code);
        apiHttpClient.verifyEmail(token, request, new ApiHttpClientUser.VerifyEmailCallback() {
            @Override
            public void onSuccess(VerifyEmailResponse response) {
                setLoading(false);
                if (!response.isValid()) {
                    Toast.makeText(VerifyEmailActivity.this,
                            "Código de verificación inválido",
                            Toast.LENGTH_LONG).show();
                    clearCodeInputs();
                    return;
                }
                Log.d(TAG, "Email verificado exitosamente");
                
                Toast.makeText(VerifyEmailActivity.this, 
                        "¡Email verificado correctamente!", 
                        Toast.LENGTH_LONG).show();
                
                // Navegar a MainActivity
                navigateToMain();
            }
            
            @Override
            public void onError(Exception error) {
                setLoading(false);
                Log.e(TAG, "Error verificando email", error);
                // El error ya se muestra en Toast por ErrorHandler
                
                // Limpiar campos
                clearCodeInputs();
            }
        });
    }
    
    /**
     * Reenviar código de verificación con countdown de 1 minuto
     */
    private void resendCode() {
        // Validar si puede reenviar
        if (!canResend) {
            Toast.makeText(this, 
                    "Espera antes de reenviar el código", 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        setLoading(true);
        
        apiHttpClient.resendVerification(token, new ApiHttpClientUser.ResendVerificationCallback() {
            @Override
            public void onSuccess(ResendVerificationResponse response) {
                setLoading(false);
                Log.d(TAG, "✅ Código reenviado exitosamente");
                
                Toast.makeText(VerifyEmailActivity.this, 
                        "Código reenviado. Revisa tu correo.", 
                        Toast.LENGTH_LONG).show();
                
                // Iniciar countdown de 1 minuto
                startCountdown();
                clearCodeInputs();
            }
            
            @Override
            public void onError(Exception error) {
                setLoading(false);
                Log.e(TAG, "Error reenviando código", error);
                // El error ya se muestra en Toast por ErrorHandler
            }
        });
    }
    
    /**
     * Iniciar countdown de 1 minuto
     * Muestra el tiempo restante en el botón
     */
    private void startCountdown() {
        // Cancelar countdown anterior si existe
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        
        canResend = false;
        resendButton.setEnabled(false);
        
        countDownTimer = new CountDownTimer(COUNTDOWN_TIME, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = millisUntilFinished / 1000;
                
                // Formatear tiempo como MM:SS
                long minutes = secondsRemaining / 60;
                long seconds = secondsRemaining % 60;
                String timeText = String.format("Espera %d:%02d", minutes, seconds);
                
                resendButton.setText(timeText);
                Log.d(TAG, "⏱️ Countdown: " + secondsRemaining + "s restantes");
            }
            
            @Override
            public void onFinish() {
                canResend = true;
                resendButton.setEnabled(true);
                resendButton.setText("Reenviar código");
                Log.d(TAG, "✅ Countdown finalizado - puede reenviar código");
            }
        }.start();
        
        Log.d(TAG, "⏱️ Countdown iniciado - 60 segundos");
    }
    
    /**
     * Limpiar todas las casillas de código
     */
    private void clearCodeInputs() {
        for (TextInputEditText digit : codeDigits) {
            digit.setText("");
        }
        codeDigits[0].requestFocus();
    }
    
    /**
     * Mostrar/ocultar loading
     */
    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        verifyButton.setEnabled(!loading);
        
        if (!loading) {
            resendButton.setEnabled(canResend);
        }
        
        for (TextInputEditText digit : codeDigits) {
            digit.setEnabled(!loading);
        }
    }
    
    /**
     * Navegar a MainActivity
     */
    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("TOKEN", token);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    /**
     * Navegar a Login
     */
    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    /**
     * Cerrar sesión
     */
    private void performLogout() {
        if (token == null || token.isEmpty()) {
            Log.w(TAG, "⚠️ No hay token válido, redirigiendo a login");
            navigateToLogin();
            return;
        }
        
        Log.d(TAG, "🔄 Iniciando logout...");
        Toast.makeText(this, "Cerrando sesión...", Toast.LENGTH_SHORT).show();
        
        apiHttpClient.logout(token, new ApiHttpClientUser.LogoutCallback() {
            @Override
            public void onSuccess(LogoutResponse response) {
                Log.d(TAG, "✅ Logout exitoso en el servidor");
                
                // Limpiar sesión local
                sessionManager.clearSession();
                
                Toast.makeText(VerifyEmailActivity.this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
                
                // Redirigir al login
                navigateToLogin();
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "❌ Error al cerrar sesión en el servidor", error);
                
                // Aunque falle el API, limpiar sesión local de todos modos
                sessionManager.clearSession();
                
                Toast.makeText(VerifyEmailActivity.this, "Sesión cerrada localmente", Toast.LENGTH_SHORT).show();
                
                // Redirigir al login
                navigateToLogin();
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
