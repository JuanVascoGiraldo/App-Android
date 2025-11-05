package com.example.androidchatproject.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import com.example.androidchatproject.R;
import com.example.androidchatproject.config.ApiConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Helper para cargar imágenes de perfil desde URLs con cache
 */
public class ProfileImageLoader {
    
    private static final String TAG = "ProfileImageLoader";
    private static final String CACHE_DIR = "profile_images";
    private static final int CACHE_DAYS = 7; // 7 días de validez
    private static final int CONNECT_TIMEOUT = 10000; // 10 segundos
    private static final int READ_TIMEOUT = 10000; // 10 segundos
    
    private Context context;
    private File cacheDir;
    private ExecutorService executorService;
    private Handler mainHandler;
    
    public ProfileImageLoader(Context context) {
        this.context = context.getApplicationContext();
        this.cacheDir = new File(context.getCacheDir(), CACHE_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        this.executorService = Executors.newFixedThreadPool(3); // 3 hilos concurrentes
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Cargar imagen de perfil desde URL con cache
     */
    public void loadProfileImage(String imageUrl, ImageView imageView, String userId) {
        Log.d(TAG, "loadProfileImage called - userId: " + userId + ", imageUrl: " + imageUrl);
        
        if (imageUrl == null || imageUrl.isEmpty()) {
            Log.d(TAG, "No image URL provided, loading default");
            loadDefaultImage(imageView);
            return;
        }
        
        // Primero intentar cargar desde cache
        Bitmap cachedBitmap = loadFromCache(userId);
        if (cachedBitmap != null) {
            Log.d(TAG, "Image loaded from cache for user: " + userId);
            imageView.setImageBitmap(cachedBitmap);
            return;
        }
        
        // Si no está en cache, cargar imagen por defecto mientras descarga
        Log.d(TAG, "Not in cache, loading default and starting download");
        loadDefaultImage(imageView);
        
        // Descargar en background
        executorService.execute(() -> {
            try {
                // Construir URL completa - eliminar barra al final de BASE_URL si imageUrl empieza con /
                String fullUrl;
                if (imageUrl.startsWith("http")) {
                    fullUrl = imageUrl;
                } else {
                    // Asegurar que no haya doble barra
                    String baseUrl = ApiConfig.BASE_URL.endsWith("/") ? ApiConfig.BASE_URL.substring(0, ApiConfig.BASE_URL.length() - 1) : ApiConfig.BASE_URL;
                    String imagePath = imageUrl.startsWith("/") ? imageUrl : "/" + imageUrl;
                    fullUrl = baseUrl + imagePath;
                }
                
                Log.d(TAG, "Downloading profile image from: " + fullUrl);
                
                Bitmap bitmap = downloadBitmap(fullUrl);
                
                if (bitmap != null) {
                    Log.d(TAG, "Image downloaded successfully for user: " + userId);
                    // Guardar en cache
                    saveToCache(bitmap, userId);
                    
                    // Actualizar UI en el hilo principal
                    mainHandler.post(() -> {
                        Log.d(TAG, "Updating ImageView with downloaded bitmap");
                        imageView.setImageBitmap(bitmap);
                    });
                } else {
                    Log.e(TAG, "Failed to download image from: " + fullUrl);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading profile image: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Descargar bitmap desde URL
     */
    private Bitmap downloadBitmap(String imageUrl) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        
        try {
            Log.d(TAG, "Creating connection to: " + imageUrl);
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setDoInput(true);
            connection.setRequestMethod("GET");
            
            Log.d(TAG, "Connecting...");
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (bitmap != null) {
                    Log.d(TAG, "Bitmap decoded successfully - Width: " + bitmap.getWidth() + ", Height: " + bitmap.getHeight());
                } else {
                    Log.e(TAG, "Failed to decode bitmap from stream");
                }
                return bitmap;
            } else {
                Log.e(TAG, "HTTP error code: " + responseCode + " for URL: " + imageUrl);
                return null;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error downloading bitmap from " + imageUrl + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    // Ignorar
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Cargar imagen desde cache
     */
    private Bitmap loadFromCache(String userId) {
        try {
            File cacheFile = new File(cacheDir, userId + ".jpg");
            
            if (!cacheFile.exists()) {
                return null;
            }
            
            // Verificar si el cache es válido (menos de CACHE_DAYS días)
            long cacheAge = System.currentTimeMillis() - cacheFile.lastModified();
            long maxAge = CACHE_DAYS * 24 * 60 * 60 * 1000L;
            
            if (cacheAge > maxAge) {
                // Cache expirado, eliminar
                cacheFile.delete();
                return null;
            }
            
            // Cargar desde cache
            FileInputStream fis = new FileInputStream(cacheFile);
            Bitmap bitmap = BitmapFactory.decodeStream(fis);
            fis.close();
            
            Log.d(TAG, "Loaded image from cache for user: " + userId);
            return bitmap;
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading from cache: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Guardar imagen en cache
     */
    private void saveToCache(Bitmap bitmap, String userId) {
        try {
            File cacheFile = new File(cacheDir, userId + ".jpg");
            FileOutputStream fos = new FileOutputStream(cacheFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.flush();
            fos.close();
            
            Log.d(TAG, "Saved image to cache for user: " + userId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving to cache: " + e.getMessage());
        }
    }
    
    /**
     * Cargar imagen por defecto
     */
    private void loadDefaultImage(ImageView imageView) {
        try {
            Bitmap defaultBitmap = BitmapFactory.decodeResource(
                    context.getResources(),
                    R.drawable.user_default
            );
            if (defaultBitmap != null) {
                imageView.setImageBitmap(defaultBitmap);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading default image: " + e.getMessage());
        }
    }
    
    /**
     * Limpiar cache de imágenes
     */
    public void clearCache() {
        executorService.execute(() -> {
            try {
                if (cacheDir.exists() && cacheDir.isDirectory()) {
                    File[] files = cacheDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            file.delete();
                        }
                    }
                }
                Log.d(TAG, "Cache cleared");
            } catch (Exception e) {
                Log.e(TAG, "Error clearing cache: " + e.getMessage());
            }
        });
    }
    
    /**
     * Eliminar imagen específica del cache
     */
    public void removeFromCache(String userId) {
        executorService.execute(() -> {
            try {
                File cacheFile = new File(cacheDir, userId + ".jpg");
                if (cacheFile.exists()) {
                    cacheFile.delete();
                    Log.d(TAG, "Removed image from cache for user: " + userId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error removing from cache: " + e.getMessage());
            }
        });
    }
    
    /**
     * Obtener tamaño del cache en bytes
     */
    public long getCacheSize() {
        long size = 0;
        try {
            if (cacheDir.exists() && cacheDir.isDirectory()) {
                File[] files = cacheDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        size += file.length();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating cache size: " + e.getMessage());
        }
        return size;
    }
    
    /**
     * Cerrar el executor service (llamar cuando ya no se necesite)
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
