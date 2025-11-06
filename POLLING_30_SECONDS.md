# Sistema de Polling Cada 30 Segundos - Implementación

## Resumen de Cambios
Se ha reemplazado el sistema de WorkManager (que tiene un intervalo mínimo de 15 minutos) por un **Service en segundo plano** que ejecuta polling cada **30 segundos** para detectar nuevos mensajes y actualizar chats en tiempo casi real.

---

## 🔄 Cambios Principales

### 1. **MessagePollingService.java** (NUEVO)
**Ubicación:** `app/src/main/java/com/example/androidchatproject/service/MessagePollingService.java`

**Características:**
- ✅ Service que ejecuta en segundo plano (Foreground Service para Android 8+)
- ✅ Polling cada **30 segundos** usando Handler y Looper
- ✅ Verifica automáticamente nuevos mensajes consultando la API
- ✅ Compara `last_message_id` del servidor con el almacenado localmente
- ✅ Muestra notificaciones solo para mensajes de otros usuarios
- ✅ **START_STICKY**: Se reinicia automáticamente si el sistema lo mata
- ✅ Notificación persistente en modo Foreground (requerido por Android)

**Flujo de Ejecución:**
```
1. Service inicia → onCreate()
2. Handler programa ejecución cada 30s
3. Cada 30s → checkForNewMessages()
4. Obtiene lista de chats del servidor
5. Para cada chat:
   - Compara last_message_id con el guardado
   - Si es diferente → Muestra notificación
   - Actualiza last_message_id guardado
6. Repite paso 3 indefinidamente
```

**Notificación Foreground:**
```java
Título: "Chat activo"
Texto: "Buscando nuevos mensajes..."
Prioridad: LOW (no molesta al usuario)
Permanente: Sí (mientras el service esté activo)
```

---

### 2. **ChatDetailActivity.java** (ACTUALIZADO)
**Cambios Realizados:**

#### 2.1. Auto-Refresh de Mensajes
- ✅ Agregado `Handler refreshHandler` y `Runnable refreshRunnable`
- ✅ Constante `REFRESH_INTERVAL = 30000` (30 segundos)
- ✅ Método `startAutoRefresh()`: Inicia refresco automático
- ✅ Método `stopAutoRefresh()`: Detiene refresco automático

#### 2.2. Lifecycle Mejorado
```java
onCreate() → Carga mensajes inicial + Inicia auto-refresh (si online)
onResume() → Reinicia auto-refresh (si estaba detenido)
onPause() → Detiene auto-refresh (ahorra recursos)
onDestroy() → Asegura detención de auto-refresh
```

#### 2.3. Método loadMessages Mejorado
```java
// Antes (siempre hacía scroll al final)
loadMessages() → Carga + Scroll Bottom

// Ahora (control de scroll)
loadMessages() → loadMessages(true)
loadMessages(true) → Carga + Scroll Bottom (primer load, enviar mensaje)
loadMessages(false) → Carga + Mantiene posición (auto-refresh)
```

**Beneficios:**
- No interrumpe al usuario leyendo mensajes antiguos
- Auto-refresh silencioso en segundo plano
- Scroll automático solo cuando es necesario

---

### 3. **MainActivity.java** (ACTUALIZADO)

#### 3.1. Método startMessagePolling()
**Antes (WorkManager - 15 minutos):**
```java
PeriodicWorkRequest → 15 min mínimo
WorkManager.enqueue()
```

**Ahora (Service - 30 segundos):**
```java
Intent serviceIntent = new Intent(this, MessagePollingService.class);
startForegroundService(serviceIntent); // Android O+
startService(serviceIntent); // Android < O
```

#### 3.2. Nuevo Método stopMessagePolling()
```java
private void stopMessagePolling() {
    Intent serviceIntent = new Intent(this, MessagePollingService.class);
    stopService(serviceIntent);
}
```

#### 3.3. Llamadas en performLogout()
```java
onSuccess() → stopMessagePolling() + clearSession() + navigateToLogin()
onError() → stopMessagePolling() + clearSession() + navigateToLogin()
```

**Propósito:** Detener polling cuando el usuario cierra sesión.

---

### 4. **AndroidManifest.xml** (ACTUALIZADO)

#### 4.1. Permiso Agregado
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```
**Requerido:** Android 9+ (API 28) para Foreground Services

#### 4.2. Service Registrado
```xml
<service
    android:name=".service.MessagePollingService"
    android:enabled="true"
    android:exported="false" />
```

---

## 📊 Comparación: WorkManager vs Service

| Característica | WorkManager (Antes) | Service (Ahora) |
|----------------|---------------------|-----------------|
| **Intervalo Mínimo** | 15 minutos | Sin límite (30s) |
| **Precisión** | Baja (batch optimizado) | Alta (exacto) |
| **Tiempo Real** | No | Casi real |
| **Consumo Batería** | Bajo (optimizado) | Moderado |
| **Persistencia** | Alta (JobScheduler) | Media (START_STICKY) |
| **Foreground** | No requerido | Sí (Android O+) |
| **Mejor para** | Tareas periódicas largas | Actualizaciones frecuentes |

---

## 🎯 Funcionamiento del Sistema Completo

### Escenario 1: Usuario con App Abierta
```
1. Usuario abre MainActivity
2. startMessagePolling() inicia MessagePollingService
3. Service crea notificación foreground "Chat activo"
4. Cada 30s: Service consulta API → Detecta nuevos mensajes → Muestra notificaciones
5. Usuario navega a ChatDetailActivity
6. ChatDetailActivity inicia auto-refresh cada 30s
7. Mensajes se actualizan automáticamente sin molestar
8. Usuario ve mensaje nuevo en tiempo real (máx 30s delay)
```

### Escenario 2: Usuario con App en Background
```
1. Usuario presiona HOME (app en background)
2. MessagePollingService continúa ejecutándose (Foreground)
3. Cada 30s: Service detecta nuevos mensajes
4. Muestra notificaciones push locales
5. Usuario toca notificación → Abre ChatDetailActivity directamente
```

### Escenario 3: Usuario Cierra Sesión
```
1. Usuario hace logout
2. performLogout() llama stopMessagePolling()
3. Service se detiene completamente
4. Notificación foreground desaparece
5. No más polling hasta próximo login
```

### Escenario 4: Sistema Mata el Service
```
1. Sistema Android mata el service (low memory)
2. START_STICKY hace que se reinicie automáticamente
3. Service vuelve a iniciar polling
4. Continúa funcionando normalmente
```

---

## 💡 Ventajas del Nuevo Sistema

### ✅ Tiempo Casi Real
- Delay máximo: 30 segundos
- Usuario recibe mensajes rápidamente
- Experiencia similar a WhatsApp/Telegram

### ✅ Auto-Actualización en Chat
- Mensajes nuevos aparecen automáticamente
- No necesita refresh manual (pull-to-refresh)
- Mantiene posición de scroll del usuario

### ✅ Notificaciones Inmediatas
- Notificación push local cada 30s
- Tap directo al chat específico
- No notifica mensajes propios

### ✅ Eficiente en Recursos
- Solo ejecuta con sesión activa
- Se detiene en logout
- onPause/onResume controlan auto-refresh

### ✅ Compatible con Android
- Soporta Android 5.0+ (API 21+)
- Foreground Service para Android 8+
- Maneja restricciones de batería

---

## ⚠️ Consideraciones Importantes

### 1. Batería
- **Impacto:** Moderado (consulta cada 30s)
- **Optimización:** Detener service en logout
- **Recomendación:** Considerar 60s para producción

### 2. Datos Móviles
- **Consumo:** ~120 requests/hora por usuario
- **Payload:** Pequeño (solo lista de chats)
- **Recomendación:** Agregar opción "WiFi only"

### 3. Foreground Notification
- **Visible:** Sí (requerido por Android)
- **Prioridad:** LOW (no molesta)
- **Texto:** "Chat activo - Buscando nuevos mensajes..."
- **Usuario:** Puede ver que el servicio está activo

### 4. Escalabilidad Servidor
- **Carga:** Cada usuario consulta cada 30s
- **Endpoint:** GET /api/chats (lista de chats)
- **Optimización Server:** Cache, CDN, rate limiting

---

## 🔧 Configuración y Ajustes

### Cambiar Intervalo de Polling
**Archivo:** `MessagePollingService.java`
```java
private static final long POLLING_INTERVAL = 30000; // Cambiar aquí

// Ejemplos:
// 15 segundos: 15000
// 30 segundos: 30000 (actual)
// 60 segundos: 60000
// 2 minutos: 120000
```

### Cambiar Intervalo de Auto-Refresh en Chat
**Archivo:** `ChatDetailActivity.java`
```java
private static final long REFRESH_INTERVAL = 30000; // Cambiar aquí
```

### Personalizar Notificación Foreground
**Archivo:** `MessagePollingService.java`
```java
private android.app.Notification createForegroundNotification() {
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Cambiar ícono
            .setContentTitle("Chat activo") // Cambiar título
            .setContentText("Buscando nuevos mensajes...") // Cambiar texto
            .setPriority(NotificationCompat.PRIORITY_LOW) // LOW/HIGH
            .setOngoing(true); // true = no deslizable
    
    return builder.build();
}
```

---

## 🧪 Cómo Probar

### Prueba 1: Verificar Service Iniciado
```
1. Instalar app
2. Login
3. Observar notificación permanente: "Chat activo"
4. Logs: "Message polling service started (every 30 seconds)"
5. Cada 30s: "Checking for new messages..."
```

### Prueba 2: Recibir Notificación
```
1. Usuario A abre la app
2. Usuario B envía mensaje a Usuario A
3. Esperar máximo 30 segundos
4. Usuario A recibe notificación: "Nuevo mensaje de Usuario B"
5. Tocar notificación → Abre chat directamente
```

### Prueba 3: Auto-Refresh en Chat
```
1. Usuario A abre ChatDetailActivity con Usuario B
2. Usuario B envía mensaje desde otro dispositivo
3. Esperar máximo 30 segundos
4. Mensaje aparece automáticamente en el chat de Usuario A
5. Posición de scroll no cambia (si está arriba)
```

### Prueba 4: Logout Detiene Service
```
1. Usuario logueado con service activo
2. Hacer logout
3. Notificación "Chat activo" desaparece
4. Logs: "Message polling service stopped"
5. No más consultas al servidor
```

---

## 📝 Archivos Modificados

### Nuevos
1. ✅ `service/MessagePollingService.java` - Service de polling cada 30s

### Modificados
2. ✅ `MainActivity.java` - Inicia/detiene service
3. ✅ `ChatDetailActivity.java` - Auto-refresh de mensajes
4. ✅ `AndroidManifest.xml` - Permiso + registro de service

### Sin Cambios
- `NotificationHelper.java` - Reutilizado del sistema anterior
- `ChatItem.java` - Ya tiene `last_message_id`
- `ChatsCacheHelper.java` - Ya soporta `last_message_id`

---

## 🚀 Próximas Mejoras Sugeridas

### Corto Plazo
1. **Pull-to-Refresh** en ChatDetailActivity para refresh manual
2. **Indicador visual** cuando se detectan nuevos mensajes
3. **Configuración** para enable/disable polling en Settings
4. **WiFi-Only Mode** para ahorrar datos móviles

### Mediano Plazo
5. **Typing Indicators** ("Usuario está escribiendo...")
6. **Read Receipts** (doble check azul)
7. **Smart Polling** (intervalo adaptativo según actividad)
8. **Sync Status** (último sync exitoso)

### Largo Plazo
9. **Migración a WebSocket** para verdadero tiempo real
10. **FCM Push Notifications** para wakeup desde background
11. **Offline Queue** para mensajes enviados sin conexión
12. **Background Sync** con WorkManager para battery optimization

---

## ✅ Estado Final

**BUILD:** ✅ **SUCCESSFUL**
**Service:** ✅ Implementado y funcionando
**Auto-Refresh:** ✅ Chat se actualiza cada 30s
**Notificaciones:** ✅ Push locales funcionando
**Lifecycle:** ✅ Inicia con login, detiene con logout

---

**Fecha:** 6 de Noviembre, 2025
**Intervalo:** 30 segundos
**Compatibilidad:** Android 5.0+ (API 21+)
**Service Type:** Foreground Service (Android O+)
