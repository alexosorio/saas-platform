package com.saas.platform.catalog.controller;

import com.saas.platform.catalog.security.UserPrincipal;
import com.saas.platform.catalog.service.BodegaAccesoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Bodegas asignadas a cada usuario (desde las que puede vender sin permiso especial). */
@RestController
@RequestMapping("/api/usuarios-bodegas")
@RequiredArgsConstructor
public class UsuarioBodegaController {
    private final BodegaAccesoService service;

    /** Sede con la que arranca una orden nueva y si el usuario puede elegir otra. */
    @GetMapping("/me/contexto-venta")
    public BodegaAccesoService.ContextoVenta contextoVenta(@AuthenticationPrincipal UserPrincipal user) {
        return service.contextoVenta(user.getOrganizacionId());
    }

    @GetMapping("/{usuarioId}")
    @PreAuthorize("@authz.can('usuarios','leer')")
    public List<Long> asignadas(@PathVariable Long usuarioId, @AuthenticationPrincipal UserPrincipal user) {
        return service.asignadasDe(usuarioId, user.getOrganizacionId());
    }

    /** body: { "bodegaIds": [1, 2] } — reemplaza las asignaciones del usuario */
    @PutMapping("/{usuarioId}")
    @PreAuthorize("@authz.can('usuarios','actualizar')")
    public ResponseEntity<Void> asignar(@PathVariable Long usuarioId, @RequestBody Map<String, List<Long>> body,
                                        @AuthenticationPrincipal UserPrincipal user) {
        service.asignar(usuarioId, body.get("bodegaIds"), user.getOrganizacionId());
        return ResponseEntity.noContent().build();
    }
}
