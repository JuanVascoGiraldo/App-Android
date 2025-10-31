package com.example.androidchatproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.androidchatproject.model.user.AuthRequest;
import com.example.androidchatproject.model.user.AuthResponse;
import com.example.androidchatproject.network.ApiHttpClientUser;
import com.example.androidchatproject.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Pantalla de Login
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private CheckBox rememberSessionCheckBox;
    private MaterialButton loginButton;
    private ProgressBar progressBar;
    private TextView forgotPasswordTextView;
    private TextView registerTextView;

    private ApiHttpClientUser apiHttpClient;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inicializar componentes
        initViews();
        initServices();
        checkExistingSession();
        setupListeners();
    }

    private void initViews() {
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        rememberSessionCheckBox = findViewById(R.id.rememberSessionCheckBox);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.progressBar);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);
        registerTextView = findViewById(R.id.registerTextView);
    }

    private void initServices() {
        apiHttpClient = new ApiHttpClientUser(this);
        sessionManager = new SessionManager(this);
    }

    /**
     * Verificar si ya existe una sesión guardada
     */
    private void checkExistingSession() {
        if (sessionManager.hasSession()) {
            // Ya hay sesión activa, ir directamente a MainActivity
            navigateToMain();
        }
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());

        forgotPasswordTextView.setOnClickListener(v -> {
            Toast.makeText(this, "Funcionalidad en desarrollo", Toast.LENGTH_SHORT).show();
            // TODO: Implementar recuperación de contraseña
        });

        registerTextView.setOnClickListener(v -> {
            // Navegar a pantalla de registro
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Intentar hacer login
     */
    private void attemptLogin() {
        // Limpiar errores previos
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);

        // Obtener valores
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        boolean rememberSession = rememberSessionCheckBox.isChecked();

        // Validar campos
        if (!validateInput(email, password)) {
            return;
        }

        // Mostrar loading
        setLoading(true);

        // Crear request
        AuthRequest request = new AuthRequest(email, password, rememberSession);

        // Hacer login
        apiHttpClient.login(request, new ApiHttpClientUser.LoginCallback() {
            @Override
            public void onSuccess(AuthResponse response) {
                setLoading(false);

                // Guardar token
                String token = response.getJwt();
                String expirationDate = response.getExpirationDate();
                sessionManager.saveToken(token, expirationDate, rememberSession);

                // Mostrar mensaje de éxito
                Toast.makeText(LoginActivity.this, 
                    getString(R.string.login_success), 
                    Toast.LENGTH_SHORT).show();

                // Navegar a MainActivity pasando el token por Intent
                navigateToMain(token, expirationDate);
            }

            @Override
            public void onError(Exception error) {
                setLoading(false);
                // El error ya se mostró automáticamente en Toast por ErrorHandler
                
                // Limpiar contraseña por seguridad
                passwordEditText.setText("");
                passwordEditText.requestFocus();
            }
        });
    }

    /**
     * Validar los campos de entrada
     */
    private boolean validateInput(String email, String password) {
        boolean isValid = true;

        // Validar email
        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError(getString(R.string.error_empty_email));
            emailEditText.requestFocus();
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError(getString(R.string.error_invalid_email));
            emailEditText.requestFocus();
            isValid = false;
        }

        // Validar contraseña
        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError(getString(R.string.error_empty_password));
            if (isValid) {
                passwordEditText.requestFocus();
            }
            isValid = false;
        } else if (password.length() < 6) {
            passwordInputLayout.setError(getString(R.string.error_short_password));
            if (isValid) {
                passwordEditText.requestFocus();
            }
            isValid = false;
        }

        return isValid;
    }

    /**
     * Mostrar/ocultar loading
     */
    private void setLoading(boolean loading) {
        if (loading) {
            progressBar.setVisibility(View.VISIBLE);
            loginButton.setEnabled(false);
            emailEditText.setEnabled(false);
            passwordEditText.setEnabled(false);
            rememberSessionCheckBox.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            loginButton.setEnabled(true);
            emailEditText.setEnabled(true);
            passwordEditText.setEnabled(true);
            rememberSessionCheckBox.setEnabled(true);
        }
    }

    /**
     * Navegar a MainActivity con token en Intent
     */
    private void navigateToMain(String token, String expirationDate) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("TOKEN", token);
        intent.putExtra("EXPIRATION_DATE", expirationDate);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Navegar a MainActivity sin token (cuando ya existe sesión guardada)
     */
    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Verificar sesión cada vez que se reanuda la activity
        if (sessionManager.hasSession()) {
            navigateToMain();
        }
    }
}
