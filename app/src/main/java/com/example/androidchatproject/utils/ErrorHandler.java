package com.example.androidchatproject.utils;

import android.content.Context;
import android.widget.Toast;

import com.example.androidchatproject.model.ApiError;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilidad para parsear y mostrar errores de la API en Toast
 */
public class ErrorHandler {
    
    private static final Gson gson = new Gson();
    
    // Mapa de códigos de error personalizados
    private static final Map<Integer, String> ERROR_MESSAGES = new HashMap<>();
    
    static {
        ERROR_MESSAGES.put(1000, "Usuario no encontrado");
        ERROR_MESSAGES.put(1001, "La contraseña debe contener al menos 8 caracteres, una letra minúscula, una letra mayúscula y un número");
        ERROR_MESSAGES.put(1002, "Las contraseñas no coinciden");
        ERROR_MESSAGES.put(1003, "Es necesario enviar la verificación nuevamente");
        ERROR_MESSAGES.put(1004, "El correo electrónico ya existe en la base de datos");
        ERROR_MESSAGES.put(1005, "El nombre de usuario ya existe en la base de datos");
        ERROR_MESSAGES.put(1006, "El correo electrónico ya ha sido verificado");
        ERROR_MESSAGES.put(1007, "Verificación no encontrada");
        ERROR_MESSAGES.put(1008, "Sesión inválida");
        ERROR_MESSAGES.put(1009, "La recuperación ha expirado");
        ERROR_MESSAGES.put(1010, "El atributo no es válido");
        ERROR_MESSAGES.put(1011, "El usuario ya existe");
    }
    
    /**
     * Parsea un JSON de error y lo muestra en un Toast
     * 
     * @param context Contexto de la aplicación
     * @param errorJson JSON con el error (ej: {"error_code": 1, "message": "Error"})
     * @return El objeto ApiError parseado, o null si no se pudo parsear
     */
    public static ApiError showErrorToast(Context context, String errorJson) {
        try {
            ApiError apiError = gson.fromJson(errorJson, ApiError.class);
            
            if (apiError != null && apiError.getMessage() != null) {
                String message = formatErrorMessage(apiError);
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                return apiError;
            } else {
                // Si no se pudo parsear correctamente, mostrar el JSON completo
                Toast.makeText(context, errorJson, Toast.LENGTH_LONG).show();
                return null;
            }
            
        } catch (JsonSyntaxException e) {
            // Si el JSON es inválido, mostrar el error tal cual
            Toast.makeText(context, errorJson, Toast.LENGTH_LONG).show();
            return null;
        }
    }
    
    /**
     * Parsea un JSON de error sin mostrarlo en Toast
     * 
     * @param errorJson JSON con el error
     * @return El objeto ApiError parseado, o null si no se pudo parsear
     */
    public static ApiError parseError(String errorJson) {
        try {
            return gson.fromJson(errorJson, ApiError.class);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }
    
    /**
     * Muestra un ApiError en un Toast
     * 
     * @param context Contexto de la aplicación
     * @param apiError Objeto ApiError a mostrar
     */
    public static void showError(Context context, ApiError apiError) {
        if (apiError != null) {
            String message = formatErrorMessage(apiError);
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Formatea el mensaje de error para mostrarlo al usuario
     * Busca primero en el mapa de mensajes personalizados
     * 
     * @param apiError Objeto ApiError
     * @return Mensaje formateado
     */
    private static String formatErrorMessage(ApiError apiError) {
        int errorCode = apiError.getErrorCode();
        
        // Buscar mensaje personalizado por código
        if (ERROR_MESSAGES.containsKey(errorCode)) {
            return ERROR_MESSAGES.get(errorCode);
        }
        
        // Si no hay mensaje personalizado, usar el mensaje del servidor
        if (apiError.getMessage() != null && !apiError.getMessage().isEmpty()) {
            return apiError.getMessage();
        }
        
        // Mensaje genérico si no hay nada más
        return "Error " + errorCode + ": Ocurrió un error inesperado";
    }
    
    /**
     * Muestra un error genérico cuando no hay respuesta de la API
     * 
     * @param context Contexto de la aplicación
     * @param exception Excepción capturada
     */
    public static void showNetworkError(Context context, Exception exception) {
        String message = "Error de conexión: " + exception.getMessage();
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
