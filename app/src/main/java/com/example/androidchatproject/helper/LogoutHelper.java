package com.example.androidchatproject.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.example.androidchatproject.LoginActivity;
import com.example.androidchatproject.model.user.LogoutResponse;
import com.example.androidchatproject.network.ApiHttpClientUser;
import com.example.androidchatproject.session.SessionManager;

/**
 * Helper para manejar el logout desde cualquier Activity
 */
public class LogoutHelper {
    
    private static final String TAG = "LogoutHelper";
    
    /**
     * Ejecutar logout: llama al API, limpia sesión local y redirige al login
     */
    public static void performLogout(Activity activity) {
        SessionManager sessionManager = new SessionManager(activity);
        String token = sessionManager.getToken();
        
        if (!SessionHelper.isTokenValid(token)) {
            Log.w(TAG, "No hay token válido, redirigiendo a login");
            redirectToLogin(activity);
            return;
        }
        
        Log.d(TAG, "Iniciando logout...");
        
        // Mostrar indicador de carga (opcional)
        Toast.makeText(activity, "Cerrando sesión...", Toast.LENGTH_SHORT).show();
        
        ApiHttpClientUser apiClient = new ApiHttpClientUser(activity);
        
        apiClient.logout(token, new ApiHttpClientUser.LogoutCallback() {
            @Override
            public void onSuccess(LogoutResponse response) {
                Log.d(TAG, "✅ Logout exitoso en el servidor");
                
                // Limpiar sesión local
                sessionManager.clearSession();
                
                Toast.makeText(activity, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
                
                // Redirigir al login
                redirectToLogin(activity);
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error al cerrar sesión en el servidor", error);
                
                // Aunque falle el API, limpiar sesión local de todos modos
                sessionManager.clearSession();
                
                Toast.makeText(activity, "Sesión cerrada localmente", Toast.LENGTH_SHORT).show();
                
                // Redirigir al login
                redirectToLogin(activity);
            }
        });
    }
    
    /**
     * Redirigir al LoginActivity y limpiar stack de activities
     */
    private static void redirectToLogin(Activity activity) {
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
        
        Log.d(TAG, "Redirigido a LoginActivity");
    }
}
