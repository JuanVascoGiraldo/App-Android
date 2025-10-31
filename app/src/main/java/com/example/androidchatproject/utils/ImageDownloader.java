package com.example.androidchatproject.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utilidad para descargar y guardar imágenes de perfil
 */
public class ImageDownloader {
    
    private static final String TAG = "ImageDownloader";
    private static final String APP_FOLDER = "MovilApp";
    private static final int CONNECT_TIMEOUT = 10000; // 10 segundos
    private static final int READ_TIMEOUT = 10000; // 10 segundos
    
    /**
     * Callback para operaciones asíncronas de descarga de imagen
     */
    public interface DownloadCallback {
        void onSuccess(File imageFile);
        void onError(Exception error);
    }
    
    /**
     * Descarga una imagen desde una URL y la guarda en MovilApp/
     * 
     * @param context Contexto de la aplicación
     * @param imageUrl URL de la imagen
     * @param fileName Nombre del archivo (ej: "profile_username.jpg")
     * @param callback Callback con el resultado
     */
    public static void downloadAndSaveImage(Context context, String imageUrl, String fileName, DownloadCallback callback) {
        new Thread(() -> {
            try {
                // Validar URL
                if (imageUrl == null || imageUrl.isEmpty()) {
                    throw new IllegalArgumentException("URL de imagen es nula o vacía");
                }
                
                Log.d(TAG, "Descargando imagen desde: " + imageUrl);
                
                // Descargar imagen
                Bitmap bitmap = downloadImage(imageUrl);
                
                if (bitmap == null) {
                    throw new IOException("No se pudo descargar la imagen");
                }
                
                // Guardar en disco
                File savedFile = saveImageToStorage(context, bitmap, fileName);
                
                Log.d(TAG, "Imagen guardada exitosamente en: " + savedFile.getAbsolutePath());
                
                // Callback en el hilo principal
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                        callback.onSuccess(savedFile)
                    );
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error al descargar/guardar imagen", e);
                
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                        callback.onError(e)
                    );
                }
            }
        }).start();
    }
    
    /**
     * Descarga una imagen desde una URL y retorna un Bitmap
     */
    private static Bitmap downloadImage(String imageUrl) throws IOException {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        
        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }
            
            inputStream = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            
            if (bitmap == null) {
                throw new IOException("No se pudo decodificar la imagen");
            }
            
            return bitmap;
            
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error al cerrar InputStream", e);
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Guarda un Bitmap en el almacenamiento interno de la app
     * Ruta: /storage/emulated/0/Android/data/{package}/files/MovilApp/
     */
    private static File saveImageToStorage(Context context, Bitmap bitmap, String fileName) throws IOException {
        // Obtener directorio de archivos de la app (no requiere permisos especiales)
        File appDir = context.getExternalFilesDir(null);
        
        if (appDir == null) {
            throw new IOException("No se pudo acceder al directorio de archivos externos");
        }
        
        // Crear carpeta MovilApp si no existe
        File movilAppDir = new File(appDir, APP_FOLDER);
        if (!movilAppDir.exists()) {
            boolean created = movilAppDir.mkdirs();
            if (!created) {
                throw new IOException("No se pudo crear la carpeta " + APP_FOLDER);
            }
            Log.d(TAG, "Carpeta creada: " + movilAppDir.getAbsolutePath());
        }
        
        // Crear archivo de imagen
        File imageFile = new File(movilAppDir, fileName);
        
        // Guardar bitmap como JPEG
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();
            
            Log.d(TAG, "Imagen guardada: " + imageFile.getAbsolutePath());
            Log.d(TAG, "Tamaño: " + (imageFile.length() / 1024) + " KB");
            
            return imageFile;
            
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error al cerrar FileOutputStream", e);
                }
            }
        }
    }
    
    /**
     * Obtiene la ruta completa donde se guardan las imágenes
     */
    public static File getImageDirectory(Context context) {
        File appDir = context.getExternalFilesDir(null);
        if (appDir != null) {
            return new File(appDir, APP_FOLDER);
        }
        return null;
    }
    
    /**
     * Verifica si una imagen ya existe en el almacenamiento
     */
    public static boolean imageExists(Context context, String fileName) {
        File imageDir = getImageDirectory(context);
        if (imageDir != null) {
            File imageFile = new File(imageDir, fileName);
            return imageFile.exists();
        }
        return false;
    }
    
    /**
     * Verifica si una imagen existe y es reciente (dentro del período de validez)
     * 
     * @param context Contexto de la aplicación
     * @param fileName Nombre del archivo
     * @param maxAgeInDays Edad máxima en días (ej: 7 para 7 días)
     * @return true si existe y es válida, false si no existe o está expirada
     */
    public static boolean isCacheValid(Context context, String fileName, int maxAgeInDays) {
        File imageFile = getImageFile(context, fileName);
        if (imageFile == null || !imageFile.exists()) {
            return false;
        }
        
        long lastModified = imageFile.lastModified();
        long currentTime = System.currentTimeMillis();
        long ageInMillis = currentTime - lastModified;
        long ageInDays = ageInMillis / (1000 * 60 * 60 * 24);
        
        boolean isValid = ageInDays < maxAgeInDays;
        
        Log.d(TAG, "Cache validation for " + fileName + ":");
        Log.d(TAG, "  Age: " + ageInDays + " days");
        Log.d(TAG, "  Max age: " + maxAgeInDays + " days");
        Log.d(TAG, "  Valid: " + isValid);
        
        return isValid;
    }
    
    /**
     * Obtiene la edad en días de una imagen
     */
    public static long getImageAgeInDays(Context context, String fileName) {
        File imageFile = getImageFile(context, fileName);
        if (imageFile == null || !imageFile.exists()) {
            return -1;
        }
        
        long lastModified = imageFile.lastModified();
        long currentTime = System.currentTimeMillis();
        long ageInMillis = currentTime - lastModified;
        return ageInMillis / (1000 * 60 * 60 * 24);
    }
    
    /**
     * Obtiene el archivo de imagen si existe
     */
    public static File getImageFile(Context context, String fileName) {
        File imageDir = getImageDirectory(context);
        if (imageDir != null) {
            File imageFile = new File(imageDir, fileName);
            if (imageFile.exists()) {
                return imageFile;
            }
        }
        return null;
    }
    
    /**
     * Elimina una imagen del almacenamiento
     */
    public static boolean deleteImage(Context context, String fileName) {
        File imageFile = getImageFile(context, fileName);
        if (imageFile != null && imageFile.exists()) {
            boolean deleted = imageFile.delete();
            if (deleted) {
                Log.d(TAG, "Imagen eliminada: " + fileName);
            }
            return deleted;
        }
        return false;
    }
    
    /**
     * Genera un nombre de archivo único para la imagen de perfil
     */
    public static String generateProfileImageFileName(String username) {
        return "profile_" + username + ".jpg";
    }
    
    /**
     * Sistema híbrido: Carga imagen con cache inteligente
     * 
     * Estrategia:
     * 1. Si existe cache válido (menos de maxAgeInDays) → usar cache
     * 2. Si cache expiró → descargar nueva versión
     * 3. Si descarga falla → usar cache antiguo como fallback
     * 4. Si no hay cache ni descarga funciona → error
     * 
     * @param context Contexto de la aplicación
     * @param imageUrl URL de la imagen
     * @param fileName Nombre del archivo
     * @param maxAgeInDays Edad máxima del cache en días (ej: 7)
     * @param callback Callback con el resultado
     */
    public static void loadImageWithCache(Context context, String imageUrl, String fileName, 
                                          int maxAgeInDays, DownloadCallback callback) {
        new Thread(() -> {
            try {
                // Validar URL
                if (imageUrl == null || imageUrl.isEmpty()) {
                    throw new IllegalArgumentException("URL de imagen es nula o vacía");
                }
                
                // 1. Verificar si existe cache válido
                if (isCacheValid(context, fileName, maxAgeInDays)) {
                    File cachedFile = getImageFile(context, fileName);
                    if (cachedFile != null) {
                        long age = getImageAgeInDays(context, fileName);
                        Log.d(TAG, "Using valid cache (age: " + age + " days) for " + fileName);
                        
                        // Callback en hilo principal
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                            callback.onSuccess(cachedFile)
                        );
                        return;
                    }
                }
                
                // 2. Cache no existe o está expirado, descargar nueva versión
                long oldAge = getImageAgeInDays(context, fileName);
                if (oldAge >= 0) {
                    Log.d(TAG, "Cache expired (age: " + oldAge + " days), re-downloading " + fileName);
                } else {
                    Log.d(TAG, "No cache found, downloading " + fileName);
                }
                
                try {
                    // Intentar descargar
                    Bitmap bitmap = downloadImage(imageUrl);
                    
                    if (bitmap == null) {
                        throw new IOException("No se pudo descargar la imagen");
                    }
                    
                    // Eliminar cache antiguo si existe
                    deleteImage(context, fileName);
                    
                    // Guardar nueva versión
                    File savedFile = saveImageToStorage(context, bitmap, fileName);
                    
                    Log.d(TAG, "New version downloaded and cached: " + savedFile.getAbsolutePath());
                    
                    // Callback en hilo principal
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                        callback.onSuccess(savedFile)
                    );
                    
                } catch (IOException downloadError) {
                    // 3. Descarga falló, intentar usar cache antiguo como fallback
                    Log.e(TAG, "Download failed, attempting to use old cache as fallback", downloadError);
                    
                    File oldCacheFile = getImageFile(context, fileName);
                    if (oldCacheFile != null && oldCacheFile.exists()) {
                        long age = getImageAgeInDays(context, fileName);
                        Log.d(TAG, "Using old cache as fallback (age: " + age + " days)");
                        
                        // Callback en hilo principal con cache antiguo
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                            callback.onSuccess(oldCacheFile)
                        );
                    } else {
                        // 4. No hay cache ni descarga funcionó
                        throw downloadError;
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading image with cache", e);
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                        callback.onError(e)
                    );
                }
            }
        }).start();
    }
    
    /**
     * Versión simplificada para imágenes de perfil con TTL por defecto de 7 días
     */
    public static void loadProfileImageWithCache(Context context, String imageUrl, String username, 
                                                 DownloadCallback callback) {
        String fileName = generateProfileImageFileName(username);
        loadImageWithCache(context, imageUrl, fileName, 7, callback);
    }
}
