# Subscription Service

Microservicio de **suscripción de organizaciones**: planes, módulos por plan, suscripción activa, historial de cambios (creación/upgrade/downgrade/cancelación/reactivación), métodos de pago de la organización y registro de pagos.

- **Puerto:** 8084
- **Schema:** `gets_shared` (compartido con user-service y catalog-service)
- **JWT:** valida los tokens del user-service usando el **mismo `JWT_SECRET`**
- **Tenancy:** la organización del usuario se resuelve igual que en catalog-service (`OrganizacionResolver` + header opcional `X-Organizacion-Id`)

Tablas: `planes`, `modulos`, `planes_modulos`, `suscripciones`, `historial_suscripciones`, `metodos_pago`, `pagos`.

## Endpoints

| Recurso | Rutas |
|---|---|
| Planes | `GET /api/plans`, `GET /api/plans/active`, `GET /api/plans/{id}` |
| Módulos | `GET /api/modules` |
| Suscripción | `GET /api/subscription/current`, `GET /api/subscription/all`, `POST /api/subscription`, `PUT /api/subscription/cancel`, `PUT /api/subscription/reactivate`, `PUT /api/subscription/auto-renewal`, `GET /api/subscription/{id}/history` |
| Métodos de pago | `GET /api/payment-methods`, `POST /api/payment-methods`, `PUT /api/payment-methods/{id}/default`, `DELETE /api/payment-methods/{id}` |
| Pagos | `GET /api/payments`, `GET /api/payments/subscription/{id}`, `POST /api/payments` |
