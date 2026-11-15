# 🚀 Inicio Rápido - Configuración de Email

## ⚠️ PROBLEMA COMÚN: Gmail Authentication Failed

Si ves este error:
```
jakarta.mail.AuthenticationFailedException: 535-5.7.8 Username and Password not accepted
```

**Causa:** Gmail NO acepta contraseñas normales para aplicaciones.

## ✅ Solución Rápida (5 minutos)

### Opción 1: Gmail con App Password

#### 1. Habilitar Verificación en 2 Pasos
```
https://myaccount.google.com/security
→ Verificación en dos pasos
→ Activar
```

#### 2. Generar App Password
```
https://myaccount.google.com/apppasswords
→ App: Correo
→ Dispositivo: User Service
→ Generar
→ Copiar los 16 caracteres SIN ESPACIOS
```

#### 3. Configurar
```bash
# .env
MAIL_USERNAME=tu-email@gmail.com
MAIL_PASSWORD=abcdefghijklmnop  # App Password (sin espacios)
```

#### 4. Reiniciar
```bash
docker-compose down
docker-compose up -d
```

### Opción 2: MailHog (Testing - SIN configuración)

**Más rápido y sin credenciales:**

```bash
# 1. Levantar con MailHog
docker-compose up -d

# 2. Configurar .env
MAIL_HOST=mailhog
MAIL_PORT=1025
MAIL_USERNAME=
MAIL_PASSWORD=

# 3. Ver emails en navegador
http://localhost:8025

# ✅ Listo! Los emails se capturan localmente
```

## 🎯 Inicio Rápido Completo

### Con Gmail Real
```bash
# 1. Generar App Password en Google
# 2. Configurar
cp .env.example .env
nano .env  # Agregar MAIL_USERNAME y MAIL_PASSWORD

# 3. Levantar
docker-compose up -d

# 4. Probar
curl -X POST http://localhost:8082/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email":"test@test.com",
    "password":"Test123!",
    "nombre":"Test",
    "apellido":"User"
  }'

# 5. Revisar tu email real ✅
```

### Con MailHog (Testing)
```bash
# 1. Copiar configuración de testing
cp .env.testing .env

# 2. Levantar (incluye MailHog)
docker-compose up -d

# 3. Ver emails en navegador
http://localhost:8025

# 4. Probar
curl -X POST http://localhost:8082/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"Test123!","nombre":"Test","apellido":"User"}'

# 5. Ver email en MailHog ✅
```

## 📚 Documentación Completa

- `GMAIL_SMTP.md` - Guía completa de Gmail
- `README.md` - Documentación del proyecto
- `ROLES_Y_MODULOS.md` - Sistema de roles

## 🔍 Troubleshooting

### Error: "Username and Password not accepted"
→ Ver `GMAIL_SMTP.md` sección "Generar App Password"

### Error: "Connection timeout"
→ Usar puerto 465 en vez de 587

### Emails no llegan
```bash
# Ver logs
docker-compose logs -f user-service | grep -i mail

# Verificar configuración
docker-compose exec user-service env | grep MAIL
```

## ✅ Verificación Rápida

```bash
# 1. Servicio corriendo
curl http://localhost:8082/actuator/health

# 2. PostgreSQL
docker-compose exec postgres psql -U postgres -c "\l"

# 3. MailHog (si lo usas)
curl http://localhost:8025

# 4. Logs del servicio
docker-compose logs user-service --tail=50
```

---

**Para más detalles:** Lee `GMAIL_SMTP.md`
