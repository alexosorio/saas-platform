package com.saas.platform.user.controller;

import com.saas.platform.user.dto.ParametroDTO;
import com.saas.platform.user.security.ContextoActor;
import com.saas.platform.user.service.ParametroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ParametroController {

    private final ParametroService service;
    private final ContextoActor actor;

    @GetMapping("/api/parametros")
    @PreAuthorize("@authz.can('configuracion','leer')")
    public List<ParametroDTO> listar() { return service.listar(); }

    /** body: { "valor": "45", "general": false } */
    @PutMapping("/api/parametros/{clave}")
    @PreAuthorize("@authz.can('configuracion','actualizar')")
    public ResponseEntity<Void> actualizar(@PathVariable String clave, @RequestBody Map<String, Object> body) {
        service.actualizar(clave, body.get("valor") != null ? String.valueOf(body.get("valor")) : null,
                           Boolean.TRUE.equals(body.get("general")));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/parametros/{clave}/organizacion")
    @PreAuthorize("@authz.can('configuracion','actualizar')")
    public ResponseEntity<Void> restablecer(@PathVariable String clave) {
        service.restablecer(clave);
        return ResponseEntity.noContent().build();
    }

    /** Configuración de sesión efectiva para quien consulta (cualquier usuario autenticado). */
    @GetMapping("/api/parametros/sesion")
    public Map<String, Object> sesion() {
        return Map.of("minutosInactividad", service.entero("sesion.minutos_inactividad", actor.organizacionId(), 30));
    }

    /** Datos públicos para el registro de nuevas organizaciones. */
    @GetMapping("/api/public/config")
    public Map<String, Object> publica() {
        return Map.of("diasPrueba", service.entero("suscripcion.dias_prueba", null, 15));
    }
}
