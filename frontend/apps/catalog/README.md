# saas-catalog-frontend

Micro-frontend dedicado al **catálogo** (productos, inventario, órdenes, clientes, listas de precios).

## Arquitectura

Este frontend NO maneja autenticación. La autenticación vive en la **app de seguridad** (`saas-security-frontend`).
- Si no hay `accessToken` en `localStorage`, redirige a la app de seguridad para login.
- Si hay token, lo usa para llamar al **catalog-service** (puerto 8083).

```
[saas-security-frontend :5173]  ──login──>  user-service :8082
       │  (deja accessToken en localStorage)
       └──> [saas-catalog-frontend :5174]  ──>  catalog-service :8083
       └──> [saas-subscription-frontend :5175]  ──>  subscription-service :8084
```

## Variables de entorno
```
VITE_CATALOG_API_URL=http://localhost:8083
VITE_SECURITY_APP_URL=http://localhost:5173
VITE_SUBSCRIPTION_APP_URL=http://localhost:5175
```

## Ejecutar
```
npm install
npm run dev    # puerto 5174
npm run build
```

> Los tres frontends (seguridad / catálogo / suscripción) deben servirse desde el mismo dominio para compartir el `accessToken` de `localStorage`. En desarrollo: todos en `localhost`. En producción: mismo dominio (rutas distintas) o subdominios con cookies cross-site adecuadas.
