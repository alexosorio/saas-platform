package com.saas.platform.user.controller;

import com.saas.platform.user.dto.CreateRolRequest;
import com.saas.platform.user.dto.RolResponse;
import com.saas.platform.user.dto.UpdateRolRequest;
import com.saas.platform.user.service.RolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RolController {

    private final RolService rolService;

    @GetMapping
    @PreAuthorize("@authz.can('roles','leer')")
    public List<RolResponse> list() { return rolService.getAll(); }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.can('roles','leer')")
    public RolResponse get(@PathVariable Long id) { return rolService.getById(id); }

    @PostMapping
    @PreAuthorize("@authz.can('roles','crear')")
    public ResponseEntity<RolResponse> create(@RequestBody CreateRolRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rolService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.can('roles','actualizar')")
    public RolResponse update(@PathVariable Long id, @RequestBody UpdateRolRequest req) {
        return rolService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.can('roles','eliminar')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rolService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Módulos disponibles para asignar a un rol. */
    @GetMapping("/modulos")
    @PreAuthorize("@authz.can('roles','leer')")
    public List<com.saas.platform.user.dto.ModuloResponse> modulos() { return rolService.listarModulos(); }

    /** Reemplaza la matriz de permisos (módulo × crear/leer/actualizar/eliminar) del rol. */
    @PutMapping("/{id}/permisos")
    @PreAuthorize("@authz.can('roles','actualizar')")
    public RolResponse actualizarPermisos(@PathVariable Long id,
                                          @RequestBody List<com.saas.platform.user.dto.CreateRolRequest.PermisoRequest> permisos,
                                          org.springframework.security.core.Authentication authentication) {
        Long userId = ((com.saas.platform.user.security.UserPrincipal) authentication.getPrincipal()).getId();
        return rolService.actualizarPermisos(id, permisos, userId);
    }

    /** Crea en la organización una copia editable del rol (p. ej. para adaptar un rol del sistema). */
    @PostMapping("/{id}/personalizar")
    @PreAuthorize("@authz.can('roles','crear')")
    public ResponseEntity<RolResponse> personalizar(@PathVariable Long id,
                                                    @RequestBody(required = false) java.util.Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(rolService.personalizar(id, body != null ? body.get("nombre") : null));
    }
}
