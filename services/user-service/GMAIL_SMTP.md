# 📧 Solución: Gmail SMTP Authentication Failed

## ❌ Error
```
jakarta.mail.AuthenticationFailedException: 535-5.7.8 Username and Password not accepted
```

## 🔍 Causa
Gmail **NO acepta contraseñas normales** para aplicaciones de terceros desde mayo de 2022. Debes usar una **App Password** (contraseña de aplicación).

## ✅ Solución: Configurar App Password de Gmail

### Paso 1: Habilitar Verificación en 2 Pasos

1. Ve a tu cuenta de Google: https://myaccount.google.com/
2. Menú lateral → **Seguridad**
3. Sección "Cómo inicias sesión en Google"
4. Click en **Verificación en dos pasos**
5. Seguir los pasos para habilitarla (SMS, app autenticadora, etc.)
6. **Importante:** Debe estar activada para poder generar App Passwords

### Paso 2: Generar App Password

1. Una vez habilitada la verificación en 2 pasos, vuelve a **Seguridad**
2. Busca **Contraseñas de aplicaciones** (aparece solo si tienes 2FA habilitado)
   - URL directa: https://myaccount.google.com/apppasswords
3. Puede que te pida iniciar sesión de nuevo
4. En "Selecciona la app y el dispositivo":
   - App: **Correo**
   - Dispositivo: **Otro (nombre personalizado)**
   - Nombre: `User Service CQRS` o cualquier nombre descriptivo
5. Click **Generar**
6. Google te mostrará una contraseña de 16 caracteres como:
   ```
   abcd efgh ijkl mnop
   ```
7. **¡IMPORTANTE!** Copia esta contraseña **SIN ESPACIOS**: `abcdefghijklmnop`

### Paso 3: Configurar en tu Aplicación

#### Opción A: Variable de Entorno (Recomendado)

```bash
# En tu archivo .env
MAIL_USERNAME=tu-email@gmail.com
MAIL_PASSWORD=abcdefghijklmnop  # App Password sin espacios
```

#### Opción B: Docker Compose

```yaml
# docker-compose.yml
services:
  user-service:
    environment:
      MAIL_USERNAME: tu-email@gmail.com
      MAIL_PASSWORD: abcdefghijklmnop  # App Password sin espacios
```

#### Opción C: application.yml (NO RECOMENDADO - solo para testing)

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: tu-email@gmail.com
    password: abcdefghijklmnop  # App Password sin espacios
```

### Paso 4: Reiniciar la Aplicación

```bash
# Si usas Docker
docker-compose down
docker-compose up -d

# Si ejecutas directamente
# Ctrl+C y volver a ejecutar
mvn spring-boot:run
```

## ✅ Verificar Configuración

### Test Manual

```bash
# Probar SMTP desde terminal (Linux/Mac)
openssl s_client -connect smtp.gmail.com:587 -starttls smtp

# Debería mostrar certificados SSL sin errores
```

### Test desde la Aplicación

```bash
# Endpoint de registro (envía email)
curl -X POST http://localhost:8082/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@test.com",
    "password": "Test123!",
    "nombre": "Test",
    "apellido": "User"
  }'

# Revisar logs
docker-compose logs -f user-service | grep -i mail
```

## 🔐 Configuración Completa de Gmail SMTP

### application.yml
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          ssl:
            trust: smtp.gmail.com
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000
```

### .env
```bash
MAIL_USERNAME=tu-email@gmail.com
MAIL_PASSWORD=abcdefghijklmnop  # App Password (16 caracteres sin espacios)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
APP_EMAIL_FROM=noreply@tuapp.com  # Puede ser diferente al MAIL_USERNAME
```

## 🚨 Errores Comunes

### Error 1: "Username and Password not accepted"
```
❌ Causa: Usando contraseña normal en vez de App Password
✅ Solución: Generar App Password desde Google Account
```

### Error 2: "App Password option not available"
```
❌ Causa: Verificación en 2 pasos NO habilitada
✅ Solución: Habilitar 2FA primero
```

### Error 3: "Invalid credentials" con App Password
```
❌ Causa: Espacios en la contraseña
✅ Solución: Eliminar TODOS los espacios (debe ser 16 caracteres seguidos)
```

### Error 4: "Connection timeout"
```
❌ Causa: Puerto bloqueado o configuración incorrecta
✅ Solución: 
   - Verificar que el puerto 587 no esté bloqueado
   - Usar puerto 465 con SSL si 587 falla
```

## 🔄 Alternativa: Usar Puerto 465 (SSL)

Si el puerto 587 está bloqueado:

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 465
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          ssl:
            enable: true
          socketFactory:
            port: 465
            class: javax.net.ssl.SSLSocketFactory
```

## 📱 Alternativas a Gmail

### SendGrid (Recomendado para Producción)
```yaml
spring:
  mail:
    host: smtp.sendgrid.net
    port: 587
    username: apikey
    password: ${SENDGRID_API_KEY}
```

### AWS SES
```yaml
spring:
  mail:
    host: email-smtp.us-east-1.amazonaws.com
    port: 587
    username: ${AWS_SES_USERNAME}
    password: ${AWS_SES_PASSWORD}
```

### Mailgun
```yaml
spring:
  mail:
    host: smtp.mailgun.org
    port: 587
    username: ${MAILGUN_SMTP_USERNAME}
    password: ${MAILGUN_SMTP_PASSWORD}
```

### Mailtrap (Solo Testing)
```yaml
spring:
  mail:
    host: smtp.mailtrap.io
    port: 2525
    username: ${MAILTRAP_USERNAME}
    password: ${MAILTRAP_PASSWORD}
```

## 🧪 Testing Local sin Email Real

### Opción 1: MailHog (Docker)

```yaml
# docker-compose.yml
services:
  mailhog:
    image: mailhog/mailhog
    ports:
      - "1025:1025"  # SMTP
      - "8025:8025"  # Web UI
      
  user-service:
    environment:
      MAIL_HOST: mailhog
      MAIL_PORT: 1025
      MAIL_USERNAME: ""
      MAIL_PASSWORD: ""
```

Ver emails en: http://localhost:8025

### Opción 2: Deshabilitar Email Temporalmente

```java
// EmailService.java
@Service
public class EmailService {
    
    @Value("${app.email.enabled:true}")
    private Boolean emailEnabled;
    
    public void enviarCodigoVerificacion(...) {
        if (!emailEnabled) {
            log.info("Email deshabilitado. Código: {}", codigo);
            return;
        }
        // ... envío real
    }
}
```

```yaml
# application.yml
app:
  email:
    enabled: false  # Deshabilitar emails temporalmente
```

## 📋 Checklist de Configuración

- [ ] Verificación en 2 pasos habilitada en Google
- [ ] App Password generada (16 caracteres)
- [ ] App Password sin espacios en .env
- [ ] MAIL_USERNAME correcto (tu email completo)
- [ ] MAIL_HOST = smtp.gmail.com
- [ ] MAIL_PORT = 587 (o 465 para SSL)
- [ ] Archivo .env cargado correctamente
- [ ] Aplicación reiniciada después de cambios
- [ ] Logs revisados en busca de errores

## 🎯 Ejemplo Completo Funcional

### 1. Generar App Password
```
1. https://myaccount.google.com/apppasswords
2. App: Correo
3. Dispositivo: User Service
4. Generar
5. Copiar: abcdefghijklmnop
```

### 2. Configurar .env
```bash
MAIL_USERNAME=mi-email@gmail.com
MAIL_PASSWORD=abcdefghijklmnop
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
APP_EMAIL_FROM=noreply@miapp.com
```

### 3. Cargar .env en Docker
```yaml
# docker-compose.yml
services:
  user-service:
    env_file:
      - .env
    # O explícitamente:
    environment:
      MAIL_USERNAME: ${MAIL_USERNAME}
      MAIL_PASSWORD: ${MAIL_PASSWORD}
```

### 4. Reiniciar
```bash
docker-compose down
docker-compose up -d
```

### 5. Probar
```bash
# Registro de usuario
curl -X POST http://localhost:8082/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Password123!",
    "nombre": "Test",
    "apellido": "User"
  }'

# Revisar logs
docker-compose logs user-service | grep -i "email\|mail"

# Deberías ver:
# "Email de verificación enviado a: test@example.com" ✅
```

## 🔐 Seguridad

### ¡NUNCA subas a Git!
```bash
# .gitignore
.env
.env.local
.env.*.local
application-local.yml
```

### Usar Variables de Entorno
```bash
# En producción (Heroku, AWS, etc.)
heroku config:set MAIL_USERNAME=tu-email@gmail.com
heroku config:set MAIL_PASSWORD=abcdefghijklmnop

# En Kubernetes
kubectl create secret generic mail-credentials \
  --from-literal=username=tu-email@gmail.com \
  --from-literal=password=abcdefghijklmnop
```

## 💡 Recomendaciones para Producción

1. **No usar Gmail en producción**
   - Límite de 500 emails/día
   - No diseñado para uso masivo
   - Mejor usar SendGrid, AWS SES, Mailgun

2. **Usar servicio dedicado**
   ```
   ✅ SendGrid: Gratis hasta 100 emails/día
   ✅ AWS SES: $0.10 por 1000 emails
   ✅ Mailgun: Gratis hasta 5000 emails/mes
   ```

3. **Implementar cola de emails**
   - Redis + Bull/BullMQ
   - RabbitMQ
   - AWS SQS

4. **Monitoreo**
   - Logs de emails enviados/fallidos
   - Métricas de tasa de entrega
   - Alertas de fallos

---

**¿Necesitas ayuda adicional?** Revisa los logs con:
```bash
docker-compose logs -f user-service
```
