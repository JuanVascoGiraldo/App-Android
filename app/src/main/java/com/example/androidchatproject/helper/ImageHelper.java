package com.example.androidchatproject.helper;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.ByteArrayOutputStream;

/**
 * Helper para operaciones con imágenes
 */
public class ImageHelper {
    
    private static final String TAG = "ImageHelper";
    private static final int DEFAULT_MAX_DIMENSION = 1024 * 10; // 10240px
    private static final int DEFAULT_QUALITY = 80; // 80%
    
    /**
     * Comprimir imagen a JPEG con configuración por defecto
     * @param bitmap Imagen a comprimir
     * @return Bytes de la imagen comprimida
     */
    public static byte[] compressImage(Bitmap bitmap) {
        return compressImage(bitmap, DEFAULT_MAX_DIMENSION, DEFAULT_QUALITY);
    }
    
    /**
     * Comprimir imagen a JPEG con dimensión máxima personalizada
     * @param bitmap Imagen a comprimir
     * @param maxDimension Dimensión máxima (ancho o alto)
     * @return Bytes de la imagen comprimida
     */
    public static byte[] compressImage(Bitmap bitmap, int maxDimension) {
        return compressImage(bitmap, maxDimension, DEFAULT_QUALITY);
    }
    
    /**
     * Comprimir imagen a JPEG con configuración personalizada
     * @param bitmap Imagen a comprimir
     * @param maxDimension Dimensión máxima (ancho o alto)
     * @param quality Calidad de compresión (0-100)
     * @return Bytes de la imagen comprimida
     */
    public static byte[] compressImage(Bitmap bitmap, int maxDimension, int quality) {
        if (bitmap == null) {
            Log.e(TAG, "[ERROR] Bitmap is null, cannot compress");
            return new byte[0];
        }
        
        Bitmap processedBitmap = bitmap;
        
        // Redimensionar si es muy grande
        if (bitmap.getWidth() > maxDimension || bitmap.getHeight() > maxDimension) {
            float scale = Math.min(
                (float) maxDimension / bitmap.getWidth(),
                (float) maxDimension / bitmap.getHeight()
            );
            
            int newWidth = Math.round(bitmap.getWidth() * scale);
            int newHeight = Math.round(bitmap.getHeight() * scale);
            
            Log.d(TAG, "📐 Resizing image:");
            Log.d(TAG, "  Original: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            Log.d(TAG, "  New: " + newWidth + "x" + newHeight);
            Log.d(TAG, "  Scale: " + (scale * 100) + "%");
            
            processedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        }
        
        // Comprimir a JPEG
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        processedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
        
        byte[] result = outputStream.toByteArray();
        
        Log.d(TAG, "[SUCCESS] Image compressed:");
        Log.d(TAG, "  Quality: " + quality + "%");
        Log.d(TAG, "  Size: " + (result.length / 1024) + " KB");
        
        return result;
    }
    
    /**
     * Calcular el tamaño de un bitmap en bytes
     * @param bitmap Imagen
     * @return Tamaño en bytes
     */
    public static long getBitmapSize(Bitmap bitmap) {
        if (bitmap == null) {
            return 0;
        }
        return bitmap.getByteCount();
    }
    
    /**
     * Calcular el tamaño de un bitmap en KB
     * @param bitmap Imagen
     * @return Tamaño en KB
     */
    public static long getBitmapSizeKB(Bitmap bitmap) {
        return getBitmapSize(bitmap) / 1024;
    }
    
    /**
     * Verificar si una imagen necesita redimensionarse
     * @param bitmap Imagen
     * @param maxDimension Dimensión máxima permitida
     * @return true si necesita redimensionarse
     */
    public static boolean needsResize(Bitmap bitmap, int maxDimension) {
        if (bitmap == null) {
            return false;
        }
        return bitmap.getWidth() > maxDimension || bitmap.getHeight() > maxDimension;
    }
    
    /**
     * Comprimir imagen a PNG (sin pérdida)
     * @param bitmap Imagen a comprimir
     * @return Bytes de la imagen comprimida
     */
    public static byte[] compressToPNG(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "[ERROR] Bitmap is null, cannot compress");
            return new byte[0];
        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        
        byte[] result = outputStream.toByteArray();
        
        Log.d(TAG, "[SUCCESS] Image compressed to PNG:");
        Log.d(TAG, "  Size: " + (result.length / 1024) + " KB");
        
        return result;
    }
}
