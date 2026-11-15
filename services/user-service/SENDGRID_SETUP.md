# 🚀 SendGrid - Configuración Rápida (5 minutos)

## ✅ Por qué SendGrid

- ✅ **100 emails/día GRATIS** para siempre
- ✅ **Sin tarjeta de crédito** para free tier
- ✅ **5 minutos** de configuración
- ✅ **Dashboard completo** con estadísticas
- ✅ **98% deliverability** (mejor que Gmail)
- ✅ **Muy fácil de usar**

## 📝 Paso a Paso (5 minutos)

### 1. Crear Cuenta (2 minutos)

```
1. Ir a: https://signup.sendgrid.com/
2. Completar formulario:
   - Email
   - Password
   - Nombre/Empresa
3. Verificar email (check inbox)
4. ✅ Listo, ya tienes cuenta
```

### 2. Generar API Key (1 minuto)

```
1. Login a SendGrid
2. Settings → API Keys
3. Click "Create API Key"
4. Configurar:
   - Name: "User Service Production"
   - API Key Permissions: "Full Access" 
   - (o "Restricted Access" → "Mail Send" solo)
5. Click "Create & View"
6. ⚠️ COPIAR LA KEY (solo se muestra una vez):
   SG.xxxxxxxxxxxxxxxxxxxxxxxxx
7. ✅ Guardar en un lugar seguro
```

### 3. Configurar en tu Aplicación (2 minutos)

#### Opción A: Variables de Entorno (Recomendado)

```bash
# .env
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=SG.xxxxxxxxxxxxxxxxxxxxxxxxx  # Tu API Key aquí
```

#### Opción B: application.yml directo (Solo para testing)

```yaml
spring:
  mail:
    host: smtp.sendgrid.net
    port: 587
    username: apikey  # Literal "apikey"
    password: SG.xxxxxxxxxxxxxxxxxxxxxxxxx  # Tu API Key
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
```

### 4. Reiniciar y Probar

```bash
# Reiniciar servicio
docker-compose restart user-service

# O si no usas Docker
mvn spring-boot:run

# Probar registro
curl -X POST http://localhost:8082/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email":"tu-email@gmail.com",
    "password":"Test123!",
    "nombre":"Test",
    "apellido":"User"
  }'

# ✅ Deberías recibir el email en segundos
```

## 🔍 Verificar que Funciona

### Ver en SendGrid Dashboard

```
1. Login a SendGrid
2. Activity → Email Activity
3. Deberías ver tu email enviado
4. Status: "Delivered" = ✅
5. Status: "Bounced" = ❌ (email no existe)
```

### Ver Logs en tu Aplicación

```bash
# Ver logs
docker-compose logs -f user-service | grep -i sendgrid

# O sin Docker
tail -f logs/application.log | grep -i mail

# Deberías ver algo como:
# "Email enviado a: tu-email@gmail.com"
```

## ⚙️ Configuración Completa

### docker-compose.yml

```yaml
services:
  user-service:
    environment:
      # SendGrid
      MAIL_HOST: smtp.sendgrid.net
      MAIL_PORT: 587
      MAIL_USERNAME: apikey
      MAIL_PASSWORD: ${SENDGRID_API_KEY}
```

### .env

```bash
# SendGrid API Key
SENDGRID_API_KEY=SG.xxxxxxxxxxxxxxxxxxxxxxxxx

# App Email From
APP_EMAIL_FROM=noreply@tudominio.com
```

### application.yml

```yaml
spring:
  mail:
    host: ${MAIL_HOST:smtp.sendgrid.net}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:apikey}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000

app:
  email:
    from: ${APP_EMAIL_FROM:noreply@tuapp.com}
```

## 🎯 Verificar Dominio (Opcional - Mejor Deliverability)

### ¿Por qué verificar tu dominio?

- ✅ Emails **NO van a spam**
- ✅ **98%+ deliverability**
- ✅ Puedes usar **tu@tudominio.com** como remitente
- ✅ Mejor reputación

### Pasos:

```
1. SendGrid → Settings → Sender Authentication
2. Click "Authenticate Your Domain"
3. DNS Provider: Seleccionar el tuyo (GoDaddy, Cloudflare, etc.)
4. Domain: tudominio.com
5. SendGrid te dará registros DNS para agregar:
   
   DNS Records a agregar:
   - 3 CNAME records (para DKIM)
   - 1 TXT record (para SPF)
   
6. Agregar los registros en tu proveedor DNS
7. Volver a SendGrid y click "Verify"
8. ✅ Esperar 24-48 horas para propagación

Ejemplo registros DNS:
┌──────────────────────────────────────────────────┐
│ Type  │ Host                    │ Value          │
├───────┼─────────────────────────┼────────────────┤
│ CNAME │ s1._domainkey          │ s1.domainkey..│
│ CNAME │ s2._domainkey          │ s2.domainkey..│
│ CNAME │ em123                  │ u123.wl.send..│
│ TXT   │ @                      │ v=spf1 incl...│
└──────────────────────────────────────────────────┘
```

## 📊 Monitorear Uso

### Dashboard SendGrid

```
1. Dashboard → Overview
2. Ver gráficas:
   - Emails enviados hoy
   - Delivered
   - Bounces
   - Opens (si usas tracking)
   - Clicks

3. Límites del Free Tier:
   - 100 emails/día
   - 3,000 emails/mes
   - Si llegas al límite, se pausan hasta el día siguiente
```

### Alertas

```
1. Settings → Alerts
2. Crear alerta:
   - "When I reach 80% of my daily limit"
   - Email: tu-email@gmail.com
3. ✅ Recibirás notificación cuando te acerques al límite
```

## 🚨 Troubleshooting

### Error: "API key does not have permissions"

```
✅ Solución:
1. SendGrid → Settings → API Keys
2. Edit tu key
3. Cambiar a "Full Access"
4. O dar permiso específico "Mail Send"
5. Guardar
```

### Error: "Sender address rejected"

```
✅ Solución:
1. Verificar que APP_EMAIL_FROM sea válido
2. O verificar tu dominio (ver arriba)
3. O usar "noreply@tudominio.com"
```

### Emails no llegan

```
✅ Checklist:
1. Ver SendGrid Activity → Email Activity
2. Status: "Delivered" = Llegó al servidor del destinatario
3. Status: "Bounced" = Email no existe
4. Revisar carpeta SPAM del destinatario
5. Verificar dominio para evitar spam (ver arriba)
```

### Llegó a SPAM

```
✅ Solución:
1. Verificar tu dominio (SPF/DKIM)
2. Evitar palabras spam: "FREE", "CLICK HERE", "URGENT"
3. Agregar texto plano (no solo HTML)
4. No enviar demasiado rápido (rate limiting)
```

## 💰 Planes y Escalado

### Free Forever
```
100 emails/día (3,000/mes)
✅ Perfecto para: MVP, testing, apps pequeñas
```

### Essentials - $19.95/mes
```
50,000 emails/mes
Email API
Basic analytics
✅ Perfecto para: Startups creciendo
```

### Pro - $89.95/mes
```
100,000 emails/mes
Advanced analytics
Dedicated IPs
Subuser management
✅ Perfecto para: Empresas establecidas
```

### Cuándo escalar

```
Si envías más de 80 emails/día consistentemente
→ Considera Essentials ($19.95/mes)

Si envías más de 1,500 emails/día
→ Considera Pro ($89.95/mes)

Si envías más de 3,000 emails/día
→ Considera Enterprise (custom pricing)
```

## 🎁 Features Útiles de SendGrid

### 1. Email Templates

```
Marketing → Email Templates → Create Template
- Editor drag & drop
- Variables dinámicas
- Guardar y reutilizar
```

### 2. Webhooks (Event Notifications)

```
Settings → Mail Settings → Event Webhooks
- Delivered
- Bounced
- Opened
- Clicked
→ Tu endpoint recibe notificaciones en tiempo real
```

### 3. Email Validation API

```
Validar si un email existe ANTES de enviar
→ Evita bounces
→ Mejor reputación
```

### 4. Scheduled Sends

```
Programar emails para envío futuro
→ Útil para campañas
```

## ✅ Checklist Final

- [ ] Cuenta SendGrid creada
- [ ] API Key generada y copiada
- [ ] Configurado en .env o application.yml
- [ ] MAIL_USERNAME = "apikey" (literal)
- [ ] MAIL_PASSWORD = tu API Key
- [ ] Servicio reiniciado
- [ ] Email de prueba enviado
- [ ] Verificado en SendGrid Dashboard
- [ ] Email recibido correctamente
- [ ] (Opcional) Dominio verificado para mejor deliverability

## 🚀 Listo!

Tu aplicación ahora puede enviar emails profesionales con SendGrid.

**Próximo paso:** Verificar tu dominio para maximizar deliverability.

---

**Documentación oficial:** https://docs.sendgrid.com/
**Soporte:** https://support.sendgrid.com/
