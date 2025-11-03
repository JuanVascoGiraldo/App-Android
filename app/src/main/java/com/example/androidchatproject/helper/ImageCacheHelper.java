package com.example.androidchatproject.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Helper para manejo de caché de imágenes
 */
public class ImageCacheHelper {
    
    private static final String TAG = "ImageCacheHelper";
    private static final String IMAGE_FOLDER = "MovilApp";
    private static final String DEFAULT_IMAGE = "user_default.png";
    private static final long CACHE_VALIDITY_DAYS = 7;
    
    /**
     * Verificar si existe la imagen en caché
     */
    public static boolean imageExistsInCache(Context context, String fileName) {
        File imageFile = getImageFile(context, fileName);
        boolean exists = imageFile.exists();
        
        if (exists) {
            Log.d(TAG, "Imagen encontrada en caché: " + fileName);
        } else {
            Log.d(TAG, "Imagen NO encontrada en caché: " + fileName);
        }
        
        return exists;
    }
    
    /**
     * Verificar si la caché es válida (menor a X días)
     */
    public static boolean isCacheValid(Context context, String fileName, long maxAgeInDays) {
        File imageFile = getImageFile(context, fileName);
        
        if (!imageFile.exists()) {
            Log.d(TAG, "Imagen no existe, caché inválido");
            return false;
        }
        
        long ageInDays = getImageAgeInDays(imageFile);
        boolean isValid = ageInDays < maxAgeInDays;
        
        if (isValid) {
            Log.d(TAG, String.format("Caché válido: %d días (max: %d)", ageInDays, maxAgeInDays));
        } else {
            Log.d(TAG, String.format("Caché expirado: %d días (max: %d)", ageInDays, maxAgeInDays));
        }
        
        return isValid;
    }
    
    /**
     * Obtener edad de la imagen en días
     */
    public static long getImageAgeInDays(Context context, String fileName) {
        File imageFile = getImageFile(context, fileName);
        return getImageAgeInDays(imageFile);
    }
    
    private static long getImageAgeInDays(File imageFile) {
        if (!imageFile.exists()) {
            return -1;
        }
        
        long lastModified = imageFile.lastModified();
        long currentTime = System.currentTimeMillis();
        long ageInMillis = currentTime - lastModified;
        long ageInDays = ageInMillis / (1000 * 60 * 60 * 24);
        return ageInDays;
    }

    /**
     * Guardar imagen en caché
     */
    public static boolean saveImageToCache(Context context, String fileName, Bitmap bitmap) {
        try {
            File imageFile = getImageFile(context, fileName);
            File parentDir = imageFile.getParentFile();
            
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            FileOutputStream outputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();
            outputStream.close();
            
            Log.d(TAG, "✅ Imagen guardada en caché: " + fileName);
            Log.d(TAG, "  Path: " + imageFile.getAbsolutePath());
            Log.d(TAG, "  Size: " + (imageFile.length() / 1024) + " KB");
            
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "Error guardando imagen en caché: " + fileName, e);
            return false;
        }
    }
    
    /**
     * Cargar imagen desde caché
     */
    public static Bitmap loadImageFromCache(Context context, String fileName) {
        File imageFile = getImageFile(context, fileName);
        
        if (!imageFile.exists()) {
            Log.w(TAG, "⚠️ Imagen no existe en caché: " + fileName);
            return null;
        }
        
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            
            if (bitmap != null) {
                Log.d(TAG, "✅ Imagen cargada desde caché: " + fileName);
            } else {
                Log.e(TAG, "❌ Error decodificando imagen: " + fileName);
            }
            
            return bitmap;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error cargando imagen desde caché: " + fileName, e);
            return null;
        }
    }
    
    /**
     * Obtener imagen por defecto
     */
    public static Bitmap getDefaultImage(Context context) {
        // Primero intentar cargar desde caché
        Bitmap defaultImage = loadImageFromCache(context, DEFAULT_IMAGE);
        
        if (defaultImage == null) {
            // Si no está en caché, intentar cargar desde assets
            try {
                InputStream is = context.getAssets().open(DEFAULT_IMAGE);
                defaultImage = BitmapFactory.decodeStream(is);
                is.close();
                
                if (defaultImage != null) {
                    Log.d(TAG, "✅ Imagen por defecto cargada desde assets: " + DEFAULT_IMAGE);
                    // Guardar en caché para próximas veces
                    saveImageToCache(context, DEFAULT_IMAGE, defaultImage);
                } else {
                    Log.d(TAG, "⚠️ No se pudo decodificar la imagen desde assets");
                }
            } catch (IOException e) {
                Log.d(TAG, "⚠️ No se encontró " + DEFAULT_IMAGE + " en assets: " + e.getMessage());
            }
        }
        
        return defaultImage;
    }
    
    /**
     * Eliminar imagen de caché
     */
    public static boolean deleteImageFromCache(Context context, String fileName) {
        File imageFile = getImageFile(context, fileName);
        
        if (imageFile.exists()) {
            boolean deleted = imageFile.delete();
            
            if (deleted) {
                Log.d(TAG, "✅ Imagen eliminada de caché: " + fileName);
            } else {
                Log.e(TAG, "❌ Error eliminando imagen: " + fileName);
            }
            
            return deleted;
        }
        
        Log.w(TAG, "⚠️ Imagen no existe, no se puede eliminar: " + fileName);
        return false;
    }
    
    /**
     * Obtener File de imagen
     */
    public static File getImageFile(Context context, String fileName) {
        File directory = new File(context.getExternalFilesDir(null), IMAGE_FOLDER);
        return new File(directory, fileName);
    }
    
    /**
     * Obtener directorio de imágenes
     */
    public static File getImageDirectory(Context context) {
        File directory = new File(context.getExternalFilesDir(null), IMAGE_FOLDER);
        
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        return directory;
    }
    
    /**
     * Limpiar toda la caché
     */
    public static void clearAllCache(Context context) {
        File directory = getImageDirectory(context);
        
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
                Log.d(TAG, "🗑️ Caché limpiada: " + files.length + " archivos eliminados");
            }
        }
    }
    
    /**
     * Obtener tamaño total de la caché en bytes
     */
    public static long getCacheSizeInBytes(Context context) {
        File directory = getImageDirectory(context);
        long totalSize = 0;
        
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            
            if (files != null) {
                for (File file : files) {
                    totalSize += file.length();
                }
            }
        }
        
        Log.d(TAG, "📊 Tamaño de caché: " + (totalSize / 1024) + " KB");
        return totalSize;
    }
    
    /**
     * Generar nombre de archivo para imagen de perfil
     */
    public static String generateProfileImageFileName(String username) {
        return "profile_" + username.toLowerCase() + ".jpg";
    }
}
