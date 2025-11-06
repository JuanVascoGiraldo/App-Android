# Sistema de Notificaciones por Polling - Implementación Completa

## Resumen
Se ha implementado un sistema de notificaciones basado en polling que verifica periódicamente nuevos mensajes en el servidor y muestra notificaciones cuando detecta cambios en el `last_message_id`.

## Componentes Implementados

### 1. Modelo de Datos: ChatItem.java
**Cambios:**
- ✅ Agregado campo `lastMessageId` con anotación `@SerializedName("last_message_id")`
- ✅ Agregado getter `getLastMessageId()`
- ✅ Agregado setter `setLastMessageId(String)`
- ✅ Actualizado constructor completo para incluir el parámetro
- ✅ Actualizado método `toString()` para incluir el campo

**Propósito:** Almacenar el ID del último mensaje de cada chat para comparación.

---

### 2. Base de Datos: ChatsCacheHelper.java
**Cambios:**
- ✅ Versión de base de datos actualizada de 1 a 2
- ✅ Agregada columna `COLUMN_LAST_MESSAGE_ID = "last_message_id"`
- ✅ Actualizada sentencia CREATE TABLE para incluir la nueva columna
- ✅ Método `cacheChats()` actualizado para guardar `last_message_id`
- ✅ Método `getCachedChats()` actualizado para leer `last_message_id`
- ✅ Método `findChatByUserId()` actualizado para incluir `last_message_id`

**Propósito:** Persistir el `last_message_id` en la caché local de SQLite.

---

### 3. Helper de Notificaciones: NotificationHelper.java (NUEVO)
**Ubicación:** `app/src/main/java/com/example/androidchatproject/helpers/NotificationHelper.java`

**Funcionalidades:**
- ✅ `saveLastSeenMessageId(chatId, messageId)` - Guarda el último mensaje visto por el usuario
- ✅ `getLastSeenMessageId(chatId)` - Obtiene el último mensaje visto
- ✅ `hasNewMessage(chatId, currentMessageId)` - Compara IDs para detectar mensajes nuevos
- ✅ `getChatsWithNewMessages(Map)` - Obtiene todos los chats con mensajes nuevos
- ✅ `getNewMessagesCount(Map)` - Cuenta cuántos chats tienen mensajes nuevos
- ✅ `clearAllLastSeenMessages()` - Limpia todo el historial (útil para logout)
- ✅ `clearLastSeenMessage(chatId)` - Limpia un chat específico

**Almacenamiento:** SharedPreferences con formato `last_msg_{chatId}`

**Lógica de Detección:**
```
Si lastSeenId == null → NO hay mensaje nuevo (primera carga)
Si lastSeenId != currentMessageId → SÍ hay mensaje nuevo
```

---

### 4. Worker de Polling: MessagePollingWorker.java (NUEVO)
**Ubicación:** `app/src/main/java/com/example/androidchatproject/workers/MessagePollingWorker.java`

**Tipo:** `androidx.work.Worker` (ejecutado por WorkManager)

**Flujo de Ejecución:**
1. ✅ Verifica que el usuario esté logueado (`token` y `userId`)
2. ✅ Llama a `apiClient.getAllChats(token, callback)`
3. ✅ Para cada chat recibido:
   - Verifica que `lastMessageId` no sea null
   - Verifica que el último mensaje NO sea del usuario actual
   - Llama a `notificationHelper.hasNewMessage()` para comparar
   - Si hay mensaje nuevo → muestra notificación
   - Actualiza el `lastSeenMessageId` después de notificar
4. ✅ Espera máximo 30 segundos por la respuesta del API

**Notificaciones:**
- ✅ Canal de notificaciones: `"chat_notifications"` (Android O+)
- ✅ Título: `"Nuevo mensaje de {username}"`
- ✅ Texto: Último mensaje del chat (preview)
- ✅ Al tocar: Abre `ChatDetailActivity` con el chat específico
- ✅ Auto-cancelable: Desaparece al tocarla
- ✅ ID único por chat: Evita duplicados

**Ícono de Notificación:**
- ✅ Creado: `res/drawable/ic_notification.xml` (ícono de mensaje)

---

### 5. MainActivity.java
**Cambios:**
- ✅ Agregados imports de WorkManager y Worker
- ✅ Nuevo método `startMessagePolling()`:
  - Configura restricciones: solo con conexión de red
  - Crea `PeriodicWorkRequest` con intervalo de 15 minutos
  - Encola trabajo con nombre único `"message_polling"`
  - Política: KEEP (mantener si ya existe)
- ✅ Llamada a `startMessagePolling()` después de cargar chats (modo online)

**Intervalo de Polling:**
- Configurado: 15 minutos (mínimo de Android WorkManager)
- Para desarrollo: Puedes usar OneTimeWorkRequest en bucle con delay más corto
- Restricción: Solo ejecuta cuando hay conexión de red

---

### 6. ChatDetailActivity.java
**Cambios:**
- ✅ Agregado import de `NotificationHelper`
- ✅ Agregado campo `private NotificationHelper notificationHelper`
- ✅ Inicialización en `onCreate()`: `notificationHelper = new NotificationHelper(this)`
- ✅ Actualizado `loadMessagesFromAPI()`:
  - Después de cargar mensajes exitosamente
  - Obtiene el último mensaje de la lista
  - Llama a `notificationHelper.saveLastSeenMessageId(chatId, lastMessageId)`
  - Evita que se muestren notificaciones de mensajes ya vistos

**Propósito:** Marcar como "visto" cuando el usuario abre un chat.

---

### 7. Dependencias: build.gradle.kts
**Cambio:**
- ✅ Agregado: `implementation("androidx.work:work-runtime:2.9.0")`

**Propósito:** WorkManager para ejecutar tareas en segundo plano de forma confiable.

---

## Flujo Completo del Sistema

### Primera Vez (Sin Historial)
1. Usuario inicia la app → MainActivity carga chats
2. WorkManager programa polling cada 15 minutos
3. MessagePollingWorker obtiene lista de chats del servidor
4. Para cada chat, `hasNewMessage()` retorna `false` (no hay historial)
5. Se guarda el `lastMessageId` sin mostrar notificación
6. Usuario abre un chat → se guarda `lastSeenMessageId`

### Cuando Llega Mensaje Nuevo
1. WorkManager ejecuta MessagePollingWorker
2. Obtiene chats del servidor con nuevo `lastMessageId`
3. Compara con `lastSeenMessageId` almacenado
4. Detecta diferencia → Muestra notificación "Nuevo mensaje de {usuario}"
5. Actualiza `lastSeenMessageId` al nuevo valor
6. Usuario toca notificación → Abre ChatDetailActivity
7. ChatDetailActivity marca mensaje como visto

### Prevención de Notificaciones Propias
```java
if (lastMessageId != null && !userId.equals(currentUserId)) {
    // Solo notificar si el mensaje NO es del usuario actual
    if (notificationHelper.hasNewMessage(chatId, lastMessageId)) {
        showNotification(...);
    }
}
```

---

## Ventajas del Sistema

### ✅ Implementación Completa
- No requiere configuración de servidor adicional (FCM/WebSocket)
- Funciona con API REST existente
- Implementación 100% cliente

### ✅ Persistencia
- SharedPreferences para tracking de mensajes vistos
- SQLite para caché de chats (incluye `last_message_id`)
- Sobrevive reinicios de app

### ✅ Eficiencia
- Solo ejecuta con conexión de red (restricción de WorkManager)
- No notifica mensajes propios
- No notifica en primera carga
- WorkManager gestiona batería automáticamente

### ✅ UX Mejorada
- Notificaciones con preview del mensaje
- Tap directo al chat específico
- Auto-cancelable
- Un ID único por chat (evita spam)

---

## Limitaciones y Consideraciones

### ⚠️ Intervalo Mínimo
- WorkManager requiere mínimo 15 minutos para `PeriodicWorkRequest`
- Para intervalos más cortos (desarrollo/pruebas):
  - Usar `OneTimeWorkRequest` en bucle con delay
  - O usar `Service` / `AlarmManager` (menos recomendado)

### ⚠️ Batería
- Polling consume más batería que push notifications (FCM)
- WorkManager optimiza automáticamente (batch, doze mode, etc.)
- Considera 30-60 minutos para producción

### ⚠️ Retraso
- Notificaciones no son instantáneas (dependen del intervalo)
- Si necesitas tiempo real: considera WebSocket o FCM

### ⚠️ Escalabilidad
- Cada usuario hace polling independiente
- Para miles de usuarios simultáneos: FCM es más eficiente en servidor

---

## Cómo Probar

### 1. Prueba Básica
```
1. Inicia la app → Login
2. Observa logs: "Message polling started"
3. Envía mensaje desde otro dispositivo/usuario
4. Espera 15 minutos (o fuerza ejecución en WorkManager)
5. Deberías ver notificación
```

### 2. Prueba con Ejecución Inmediata (Debug)
```java
// En MainActivity, reemplazar PeriodicWorkRequest con:
OneTimeWorkRequest immediateRequest = new OneTimeWorkRequest.Builder(
    MessagePollingWorker.class)
    .setInitialDelay(10, TimeUnit.SECONDS) // 10 segundos de prueba
    .setConstraints(constraints)
    .build();

WorkManager.getInstance(this).enqueue(immediateRequest);
```

### 3. Ver Estado de WorkManager
```
Logcat: Filtrar por "MessagePollingWorker"
```

### 4. Forzar Ejecución (ADB)
```bash
adb shell cmd jobscheduler run -f com.example.androidchatproject 1
```

---

## Posibles Mejoras Futuras

### 📱 Corto Plazo
1. Ajustar intervalo según estado de batería
2. Agregar sonido/vibración a notificaciones
3. Agrupar notificaciones de múltiples chats
4. Mostrar imagen de perfil en notificación (Bitmap)
5. Acción rápida "Responder" desde notificación

### 🚀 Mediano Plazo
1. Migrar a FCM para notificaciones push reales
2. Implementar WebSocket para chat en tiempo real
3. Notificaciones ricas con múltiples mensajes
4. Badge count en ícono de app (launcher)
5. Do Not Disturb/Mute por chat

### 🎯 Largo Plazo
1. Notificaciones end-to-end encrypted
2. Estadísticas de engagement con notificaciones
3. Smart notifications (machine learning para timing)
4. Sync con wearables (smartwatch)

---

## Archivos Modificados/Creados

### Nuevos
- ✅ `helpers/NotificationHelper.java`
- ✅ `workers/MessagePollingWorker.java`
- ✅ `res/drawable/ic_notification.xml`

### Modificados
- ✅ `model/chats/ChatItem.java`
- ✅ `database/ChatsCacheHelper.java`
- ✅ `MainActivity.java`
- ✅ `ChatDetailActivity.java`
- ✅ `app/build.gradle.kts`

---

## Estado del Proyecto
✅ **BUILD SUCCESSFUL** - Todos los componentes compilados correctamente

## Próximos Pasos Recomendados
1. Probar en dispositivo real
2. Ajustar intervalo de polling según necesidades
3. Personalizar apariencia de notificaciones
4. Agregar settings para enable/disable notificaciones
5. Implementar "Clear All" para notificaciones

---

**Fecha de Implementación:** $(Get-Date)
**Versión de Base de Datos:** 2
**WorkManager Versión:** 2.9.0
