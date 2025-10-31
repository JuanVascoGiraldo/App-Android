package com.example.androidchatproject.network;

import java.io.IOException;

/**
 * Excepción personalizada para errores de la API
 * Contiene el código HTTP y el JSON de error del servidor
 */
public class ApiException extends IOException {
    
    private final int httpCode;
    private final String errorJson;
    
    public ApiException(int httpCode, String errorJson) {
        super("HTTP " + httpCode + ": " + errorJson);
        this.httpCode = httpCode;
        this.errorJson = errorJson;
    }
    
    /**
     * Obtiene el código HTTP de la respuesta (400, 401, 404, 500, etc.)
     */
    public int getHttpCode() {
        return httpCode;
    }
    
    /**
     * Obtiene el JSON de error del servidor
     * Ejemplo: {"error_code": 1, "message": "User not found"}
     */
    public String getErrorJson() {
        return errorJson;
    }
    
    @Override
    public String toString() {
        return "ApiException{" +
                "httpCode=" + httpCode +
                ", errorJson='" + errorJson + '\'' +
                '}';
    }
}
