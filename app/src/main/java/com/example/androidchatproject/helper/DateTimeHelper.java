package com.example.androidchatproject.helper;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Helper para manejo de fechas y conversión de UTC a hora local
 */
public class DateTimeHelper {
    
    private static final String TAG = "DateTimeHelper";
    
    /**
     * Convertir fecha UTC (ISO 8601) a hora local del dispositivo
     * 
     * @param utcDateString Fecha en formato ISO 8601 UTC (ej: "2024-03-15T10:30:00Z")
     * @return Date en hora local del dispositivo, o null si hay error
     */
    public static Date utcToLocal(String utcDateString) {
        if (utcDateString == null || utcDateString.isEmpty()) {
            return null;
        }
        
        try {
            // Formato de entrada ISO 8601 con UTC
            SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            
            // Parse la fecha UTC
            Date utcDate = utcFormat.parse(utcDateString);
            
            // El objeto Date ya está en UTC, solo necesitamos retornarlo
            // Al formatearlo con SimpleDateFormat sin especificar timezone, usará la zona local
            return utcDate;
            
        } catch (ParseException e) {
            // Intentar con formato alternativo sin 'Z' al final
            try {
                SimpleDateFormat alternateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                alternateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                return alternateFormat.parse(utcDateString);
            } catch (ParseException e2) {
                Log.e(TAG, "Error al parsear fecha UTC: " + utcDateString, e2);
                return null;
            }
        }
    }
    
    /**
     * Convertir fecha UTC a String en hora local con formato personalizado
     * 
     * @param utcDateString Fecha en formato ISO 8601 UTC
     * @param outputFormat Formato de salida (ej: "HH:mm", "dd/MM/yy HH:mm")
     * @return String formateado en hora local, o string vacío si hay error
     */
    public static String utcToLocalString(String utcDateString, String outputFormat) {
        Date localDate = utcToLocal(utcDateString);
        
        if (localDate == null) {
            return "";
        }
        
        try {
            SimpleDateFormat localFormat = new SimpleDateFormat(outputFormat, Locale.getDefault());
            // No especificamos timezone aquí, usará la del dispositivo por defecto
            return localFormat.format(localDate);
        } catch (Exception e) {
            Log.e(TAG, "Error al formatear fecha: " + utcDateString, e);
            return "";
        }
    }
    
    /**
     * Formatear fecha para mostrar en chat
     * - Si es hoy: muestra solo hora "HH:mm"
     * - Si es otro día: muestra "dd/MM/yy"
     * 
     * @param utcDateString Fecha en formato ISO 8601 UTC
     * @return String formateado para mostrar
     */
    public static String formatChatTime(String utcDateString) {
        Date localDate = utcToLocal(utcDateString);
        
        if (localDate == null) {
            return "";
        }
        
        try {
            // Obtener fecha actual
            Date now = new Date();
            SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            
            String todayString = dayFormat.format(now);
            String messageDateString = dayFormat.format(localDate);
            
            // Si es hoy, mostrar solo hora
            if (todayString.equals(messageDateString)) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                return timeFormat.format(localDate);
            } else {
                // Si es otro día, mostrar fecha
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
                return dateFormat.format(localDate);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al formatear tiempo de chat: " + utcDateString, e);
            return "";
        }
    }
    
    /**
     * Formatear fecha completa con hora
     * Formato: "dd/MM/yyyy HH:mm"
     * 
     * @param utcDateString Fecha en formato ISO 8601 UTC
     * @return String con fecha y hora en formato legible
     */
    public static String formatFullDateTime(String utcDateString) {
        return utcToLocalString(utcDateString, "dd/MM/yyyy HH:mm");
    }
    
    /**
     * Obtener diferencia de tiempo en formato legible
     * Ej: "Hace 5 minutos", "Hace 2 horas", "Hace 3 días"
     * 
     * @param utcDateString Fecha en formato ISO 8601 UTC
     * @return String con tiempo transcurrido
     */
    public static String getTimeAgo(String utcDateString) {
        Date localDate = utcToLocal(utcDateString);
        
        if (localDate == null) {
            return "";
        }
        
        long diff = new Date().getTime() - localDate.getTime();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (seconds < 60) {
            return "Hace un momento";
        } else if (minutes < 60) {
            return "Hace " + minutes + (minutes == 1 ? " minuto" : " minutos");
        } else if (hours < 24) {
            return "Hace " + hours + (hours == 1 ? " hora" : " horas");
        } else if (days < 7) {
            return "Hace " + days + (days == 1 ? " día" : " días");
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return dateFormat.format(localDate);
        }
    }
}
