# saas-subscription-frontend

Micro-frontend dedicado a la **suscripción de la organización**: planes, plan actual, historial, métodos de pago y registro de pagos.

## Arquitectura

Este frontend NO maneja autenticación: la autenticación vive en la **app de seguridad** (`saas-security-frontend`, puerto 5173).
- Si no hay `accessToken` en `localStorage`, redirige a la app de seguridad para login.
- Si hay token, lo reenvía al **subscription-service** (puerto 8084).

```
[saas-security-frontend :5173]  ──login──>  user-service :8082
       │  (deja accessToken en localStorage)
       ├──> [saas-catalog-frontend :5174]       ──>  catalog-service :8083
       └──> [saas-subscription-frontend :5175]  ──>  subscription-service :8084
```

Los tres frontends deben servirse desde el **mismo dominio** para compartir el `accessToken` de `localStorage`. En desarrollo: todos en `localhost`. En producción: mismo dominio (rutas o subdominios con cookies cross-site adecuadas).

## Variables de entorno
```
VITE_SUBSCRIPTION_API_URL=http://localhost:8084
VITE_SECURITY_APP_URL=http://localhost:5173
VITE_CATALOG_APP_URL=http://localhost:5174
```

## Funcionalidades

- **Planes**: cards comparativas con precios mensual/anual, módulos incluidos y botón para suscribirse o cambiar de plan.
- **Mi Suscripción**: plan actual, estado, fechas, toggle de auto-renovación, cancelar/reactivar, historial de cambios (creación, upgrades, downgrades, cancelaciones, reactivaciones).
- **Métodos de Pago**: tarjetas, transferencias, PayPal, Stripe; marcar como predeterminado; soft-delete.
- **Pagos**: historial y registro manual de pagos (suscripcion / upgrade / adicional).

## Ejecutar
```
npm install
npm run dev    # puerto 5175
npm run build
```
