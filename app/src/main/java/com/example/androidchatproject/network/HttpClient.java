package com.example.androidchatproject.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Cliente HTTP manual para hacer peticiones GET, POST, PUT
 * Utiliza HttpURLConnection y GSON para parsear respuestas JSON
 */
public class HttpClient {
    
    private static final int CONNECT_TIMEOUT = 15000; // 15 segundos
    private static final int READ_TIMEOUT = 15000; // 15 segundos
    private static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";
    
    private final Gson gson;
    
    public HttpClient() {
        this.gson = new GsonBuilder()
                .setLenient()
                .create();
    }
    
    /**
     * Realiza una petición GET
     * 
     * @param urlString URL del endpoint
     * @param responseClass Clase del objeto de respuesta
     * @param authToken Token de autenticación (opcional, puede ser null)
     * @param <T> Tipo de la respuesta
     * @return Objeto deserializado de la respuesta JSON
     * @throws IOException Si hay error de red o conexión
     */
    public <T> T get(String urlString, Class<T> responseClass, String authToken) throws IOException {
        HttpURLConnection connection = null;
        
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("Content-Type", CONTENT_TYPE_JSON);
            connection.setRequestProperty("Accept", "application/json");
            
            // Agregar token de autenticación si existe
            if (authToken != null && !authToken.isEmpty()) {
                connection.setRequestProperty("Authorization", authToken);
            }
            
            // Realizar la conexión
            connection.connect();
            
            // Verificar código de respuesta
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                // Leer el JSON de error del servidor
                String errorJson = readErrorResponse(connection);
                // Lanzar excepción con el JSON completo para que pueda ser parseado
                throw new ApiException(responseCode, errorJson);
            }
            
            // Leer la respuesta
            String jsonResponse = readResponse(connection);
            
            // Parsear JSON a objeto usando GSON
            return gson.fromJson(jsonResponse, responseClass);
            
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Realiza una petición POST
     * 
     * @param urlString URL del endpoint
     * @param requestBody Objeto que será convertido a JSON
     * @param responseClass Clase del objeto de respuesta
     * @param authToken Token de autenticación (opcional, puede ser null)
     * @param <T> Tipo de la respuesta
     * @return Objeto deserializado de la respuesta JSON
     * @throws IOException Si hay error de red o conexión
     */
    public <T> T post(String urlString, Object requestBody, Class<T> responseClass, String authToken) throws IOException {
        return sendRequestWithBody("POST", urlString, requestBody, responseClass, authToken);
    }
    
    /**
     * Realiza una petición PUT
     * 
     * @param urlString URL del endpoint
     * @param requestBody Objeto que será convertido a JSON
     * @param responseClass Clase del objeto de respuesta
     * @param authToken Token de autenticación (opcional, puede ser null)
     * @param <T> Tipo de la respuesta
     * @return Objeto deserializado de la respuesta JSON
     * @throws IOException Si hay error de red o conexión
     */
    public <T> T put(String urlString, Object requestBody, Class<T> responseClass, String authToken) throws IOException {
        return sendRequestWithBody("PUT", urlString, requestBody, responseClass, authToken);
    }
    
    /**
     * Método genérico para enviar peticiones con body (POST, PUT)
     */
    private <T> T sendRequestWithBody(String method, String urlString, Object requestBody, 
                                      Class<T> responseClass, String authToken) throws IOException {
        HttpURLConnection connection = null;
        
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("Content-Type", CONTENT_TYPE_JSON);
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true); // Permite enviar body
            
            // Agregar token de autenticación si existe
            if (authToken != null && !authToken.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + authToken);
            }
            
            // Convertir el objeto request a JSON usando GSON
            String jsonBody = gson.toJson(requestBody);
            
            // Escribir el body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }
            
            // Verificar código de respuesta
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                // Leer el JSON de error del servidor
                String errorJson = readErrorResponse(connection);
                // Lanzar excepción con el JSON completo para que pueda ser parseado
                throw new ApiException(responseCode, errorJson);
            }
            
            // Leer la respuesta
            String jsonResponse = readResponse(connection);
            
            // Parsear JSON a objeto usando GSON
            return gson.fromJson(jsonResponse, responseClass);
            
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Lee la respuesta exitosa del servidor
     */
    private String readResponse(HttpURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        
        return response.toString();
    }
    
    /**
     * Lee la respuesta de error del servidor
     */
    private String readErrorResponse(HttpURLConnection connection) {
        try {
            StringBuilder response = new StringBuilder();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            return response.toString();
            
        } catch (Exception e) {
            try {
                return connection.getResponseMessage();
            } catch (IOException ioException) {
                return "Error: " + e.getMessage();
            }
        }
    }
    
    /**
     * Obtiene la instancia de GSON utilizada por el cliente
     * Útil para parsear manualmente si es necesario
     */
    public Gson getGson() {
        return gson;
    }
}
