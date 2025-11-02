# Sistema de Verificación de Email

## 📋 Descripción General

Sistema completo de verificación de email con código de 6 dígitos, implementado con Material Design 3 y countdown de 1 minuto para reenvío.

## 🎯 Flujo de Usuario

```
Login/Registro exitoso
    ↓
MainActivity carga perfil
    ↓
¿Email verificado? ──NO──> VerifyEmailActivity
    │                           ↓
    │                      Usuario ingresa código
    │                           ↓
    │                      ¿Código correcto?
    │                           ↓ SÍ
    │                      Navegar a MainActivity
    ↓ SÍ
Mostrar contenido completo
(Hola {username} + imagen circular)
```

## 🔧 Componentes Implementados

### 1. **Modelos de Datos**

#### `VerifyEmailRequest.java`
```java
{
  "verification_code": "123456"
}
```

#### `VerifyEmailResponse.java`
```java
{
  "message": "Email verificado correctamente",
  "success": true
}
```

#### `ResendVerificationResponse.java`
```java
{
  "message": "Código reenviado",
  "success": true
}
```

### 2. **API Endpoints**

#### Verificar Email
- **Endpoint:** `POST /api/verify-email`
- **Headers:** `Authorization: Bearer {token}`
- **Body:** `VerifyEmailRequest`
- **Response:** `VerifyEmailResponse`

#### Reenviar Código
- **Endpoint:** `POST /api/resend-verification`
- **Headers:** `Authorization: Bearer {token}`
- **Body:** Ninguno (solo token)
- **Response:** `ResendVerificationResponse`

### 3. **UI Components**

#### `activity_verify_email.xml`
- ✅ Icono de email (Material Design)
- ✅ Título: "Verifica tu correo"
- ✅ Mensaje descriptivo
- ✅ **6 casillas** para código (50dp x 60dp cada una)
- ✅ Botón "Verificar código"
- ✅ Texto "¿No recibiste el código?"
- ✅ Botón "Reenviar código" con countdown
- ✅ ProgressBar para loading

#### `code_input_background.xml`
- Fondo para casillas de código
- Borde redondeado (8dp)
- Borde de 2dp con color primario

### 4. **VerifyEmailActivity.java**

#### Características Principales:

1. **Auto-avance entre casillas**
   - Al escribir un dígito → avanza automáticamente
   - Backspace → retrocede a la casilla anterior
   - Límite de 1 dígito por casilla

2. **Validación de código**
   - Debe tener exactamente 6 dígitos
   - Solo acepta números (0-9)
   - Validación antes de enviar

3. **Countdown de 1 minuto**
   - Inicia después de reenviar código
   - Botón deshabilitado durante countdown
   - Muestra "Reenviar código (59s, 58s, ...)"
   - Al finalizar → habilita botón nuevamente

4. **Estados de Loading**
   - ProgressBar visible durante peticiones
   - Botones deshabilitados
   - Casillas de código deshabilitadas

5. **Navegación**
   - Email verificado → `MainActivity` (con token)
   - Error/sin token → `LoginActivity`

## 📱 Integración con MainActivity

### Verificación Automática

```java
private void getUserProfileExample(String token) {
    // 1. Obtener perfil
    apiHttpClient.getUserProfile(token, callback {
        
        // 2. Verificar email
        if (!response.isEmailVerified()) {
            navigateToVerifyEmail(token);
            return; // ⛔ DETENER ejecución
        }
        
        // 3. Mostrar contenido (solo si verificado)
        welcomeTextView.setText("Hola, " + username);
        downloadProfileImageIfAvailable(response);
    });
}
```

### Comportamiento

| Estado del Email | Acción |
|-----------------|--------|
| `email_is_verified: false` | Redirigir a `VerifyEmailActivity` |
| `email_is_verified: true` | Mostrar contenido normal |

## 🎨 Diseño UI

### Casillas de Código
```
┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐
│ 1 │ │ 2 │ │ 3 │ │ 4 │ │ 5 │ │ 6 │
└───┘ └───┘ └───┘ └───┘ └───┘ └───┘
```
- Tamaño: 50dp x 60dp
- Espaciado: 4dp entre casillas
- Fuente: 24sp, negrita
- Centrado: texto en el centro
- Input: solo números, 1 carácter máximo

### Botón Reenviar
```
Estados:
- Normal: "Reenviar código"
- Countdown: "Reenviar código (47s)"
- Deshabilitado durante 60 segundos
```

## 🔄 Flujo Técnico Completo

### 1. Usuario hace login
```
LoginActivity → MainActivity
    ↓
Intent extras: TOKEN, EXPIRATION_DATE
```

### 2. MainActivity verifica email
```
onCreate()
    ↓
getUserProfileExample(token)
    ↓
GET /api/ → UserProfileResponse
    ↓
¿email_is_verified = false?
    ↓ SÍ
navigateToVerifyEmail(token)
```

### 3. Usuario ingresa código
```
VerifyEmailActivity
    ↓
Usuario escribe: 1-2-3-4-5-6
    ↓ (auto-avance entre casillas)
Click "Verificar código"
    ↓
POST /api/verify-email
    Body: {"verification_code": "123456"}
    Headers: Authorization: Bearer {token}
```

### 4. Respuestas del servidor

#### ✅ Código correcto
```json
{
  "message": "Email verificado correctamente",
  "success": true
}
```
→ Navegar a `MainActivity` (email ahora verificado)

#### ❌ Código incorrecto
```json
{
  "error_code": 1005,
  "message": "Código de verificación inválido"
}
```
→ Mostrar error en Toast
→ Limpiar casillas
→ Usuario puede reintentar

### 5. Reenvío de código
```
Click "Reenviar código"
    ↓
POST /api/resend-verification
    Headers: Authorization: Bearer {token}
    ↓
Countdown 60 segundos
    ↓
Botón habilitado nuevamente
```

## 📝 Notas Técnicas

### TextWatcher para Auto-avance
```java
codeDigits[i].addTextChangedListener(new TextWatcher() {
    public void onTextChanged(CharSequence s, ...) {
        if (s.length() == 1 && index < 5) {
            codeDigits[index + 1].requestFocus();
        }
    }
});
```

### OnKeyListener para Backspace
```java
codeDigits[i].setOnKeyListener((v, keyCode, event) -> {
    if (keyCode == KeyEvent.KEYCODE_DEL) {
        if (isEmpty && index > 0) {
            codeDigits[index - 1].requestFocus();
            codeDigits[index - 1].setText("");
        }
    }
});
```

### CountDownTimer
```java
new CountDownTimer(60000, 1000) {
    onTick(millisUntilFinished) {
        button.setText("Reenviar (" + seconds + "s)");
    }
    onFinish() {
        button.setEnabled(true);
    }
}.start();
```

## ✅ Testing Checklist

- [ ] Email no verificado → Redirige a VerifyEmailActivity
- [ ] Email verificado → Muestra MainActivity completo
- [ ] Auto-avance entre casillas funciona
- [ ] Backspace retrocede correctamente
- [ ] Validación: solo 6 dígitos numéricos
- [ ] Código correcto → Navega a MainActivity
- [ ] Código incorrecto → Muestra error y limpia
- [ ] Reenviar código → Inicia countdown
- [ ] Countdown 60 segundos funciona
- [ ] Después de countdown → Botón habilitado
- [ ] Loading states funcionan correctamente
- [ ] Toast muestra errores del servidor

## 🎯 Resultados

### Antes (sin verificación)
```
Login → MainActivity
    ↓
Muestra contenido inmediatamente
```

### Ahora (con verificación)
```
Login → MainActivity → Verifica email
    ↓                           ↓
    ↓                      NO verificado
    ↓                           ↓
    ↓                   VerifyEmailActivity
    ↓                           ↓
    ↓                   Ingresa código (6 dígitos)
    ↓                           ↓
    ↓←──────────────────── Código correcto
    ↓
Muestra contenido (Hola {username} + imagen circular)
```

## 🚀 Próximos Pasos (Opcionales)

1. **Agregar temporizador de expiración del código**
   - Ejemplo: código válido por 15 minutos
   
2. **Intentos máximos**
   - Bloquear después de 3 intentos fallidos
   
3. **Biometría**
   - Agregar opción de verificación por huella/Face ID
   
4. **Notificación push**
   - Enviar código también por push notification
   
5. **SMS alternativo**
   - Opción de recibir código por SMS

---

**Implementación completada:** ✅  
**Compilación exitosa:** ✅  
**Ready para testing:** ✅
