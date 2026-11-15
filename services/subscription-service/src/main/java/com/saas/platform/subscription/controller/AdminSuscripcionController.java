package com.saas.platform.subscription.controller;

import com.saas.platform.subscription.dto.AdminDTOs.*;
import com.saas.platform.subscription.security.UserPrincipal;
import com.saas.platform.subscription.service.AdminSuscripcionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Administración de suscripciones de TODAS las organizaciones.
 * Requiere el permiso de plataforma plataforma_suscripciones (el comodín '*' no lo concede).
 */
@RestController
@RequestMapping("/api/admin/subscriptions")
@RequiredArgsConstructor
public class AdminSuscripcionController {

    private final AdminSuscripcionService service;

    @GetMapping
    @PreAuthorize("@authz.plataforma('leer')")
    public List<SuscripcionAdmin> suscripciones(@RequestParam(required = false) String estado,
                                                @RequestParam(required = false) String q) {
        return service.listarSuscripciones(estado, q);
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("@authz.plataforma('actualizar')")
    public ResponseEntity<Void> activar(@PathVariable Long id, @RequestBody(required = false) Motivo body,
                                        @AuthenticationPrincipal UserPrincipal user) {
        service.activar(id, body != null ? body.getMotivo() : null, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/suspend")
    @PreAuthorize("@authz.plataforma('actualizar')")
    public ResponseEntity<Void> suspender(@PathVariable Long id, @RequestBody Motivo body,
                                          @AuthenticationPrincipal UserPrincipal user) {
        service.suspender(id, body.getMotivo(), user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/payments")
    @PreAuthorize("@authz.plataforma('leer')")
    public List<PagoAdmin> pagos(@RequestParam(required = false) String estado) {
        return service.listarPagos(estado);
    }

    @PutMapping("/payments/{id}/approve")
    @PreAuthorize("@authz.plataforma('actualizar')")
    public PagoAdmin aprobar(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        return service.aprobarPago(id, user.getId());
    }

    @PutMapping("/payments/{id}/reject")
    @PreAuthorize("@authz.plataforma('actualizar')")
    public PagoAdmin rechazar(@PathVariable Long id, @RequestBody Motivo body,
                              @AuthenticationPrincipal UserPrincipal user) {
        return service.rechazarPago(id, body.getMotivo(), user.getId());
    }

    @GetMapping("/billing")
    @PreAuthorize("@authz.plataforma('leer')")
    public Facturacion facturacion(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.facturacion(desde, hasta);
    }

    /** Ejecuta ahora el proceso de suspensión por no pago (el mismo que corre cada día). */
    @PostMapping("/run-expirations")
    @PreAuthorize("@authz.plataforma('actualizar')")
    public ResultadoVencimientos ejecutarVencimientos() {
        return service.procesarVencimientos();
    }
}
