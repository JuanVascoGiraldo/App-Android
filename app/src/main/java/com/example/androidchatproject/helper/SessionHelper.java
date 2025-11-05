package com.example.androidchatproject.helper;

import android.content.Context;
import android.util.Log;

import com.example.androidchatproject.session.SessionManager;

/**
 * Helper para manejo de sesiones y tokens
 */
public class SessionHelper {
    
    private static final String TAG = "SessionHelper";
    
    /**
     * Validar si el token es válido (no nulo y no vacío)
     */
    public static boolean isTokenValid(String token) {
        boolean isValid = token != null && !token.isEmpty();
        if (!isValid) {
            Log.w(TAG, "Token inválido: " + (token == null ? "null" : "vacío"));
        }
        return isValid;
    }
    
    /**
     * Verificar si existe una sesión activa
     */
    public static boolean hasActiveSession(Context context) {
        SessionManager sessionManager = new SessionManager(context);
        boolean hasSession = sessionManager.hasSession();
        Log.d(TAG, hasSession ? "Sesión activa encontrada" : "No hay sesión activa");
        return hasSession;
    }
    
    /**
     * Obtener token desde SessionManager
     */
    public static String getToken(Context context) {
        SessionManager sessionManager = new SessionManager(context);
        String token = sessionManager.getToken();
        
        if (isTokenValid(token)) {
            Log.d(TAG, "Token obtenido desde SessionManager");
        } else {
            Log.w(TAG, "No se pudo obtener token válido");
        }
        
        return token;
    }
    
    /**
     * Verificar si el token viene del Intent (login reciente)
     */
    public static boolean isComingFromLogin(String intentToken) {
        boolean fromLogin = isTokenValid(intentToken);
        Log.d(TAG, fromLogin ? "Usuario viene desde login" : "Usuario viene desde sesión guardada");
        return fromLogin;
    }
    
    /**
     * Cerrar sesión
     */
    public static void logout(Context context) {
        SessionManager sessionManager = new SessionManager(context);
        sessionManager.clearSession();
        Log.d(TAG, "Sesión cerrada");
    }
}
