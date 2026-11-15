package com.saas.platform.catalog.controller;

import com.saas.platform.catalog.dto.AccesoEnlaceRequest;
import com.saas.platform.catalog.dto.VistaPublicaDTO;
import com.saas.platform.catalog.service.EnlacePublicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints PUBLICOS (sin autenticacion). Permitidos en SecurityConfig
 * via .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll().
 *
 * El POST tambien debe permitirse (lo agregamos en SecurityConfig).
 */
@RestController
@RequestMapping("/api/public/lists")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class PublicoController {

    private final EnlacePublicoService service;

    /** Acceso a un enlace publico SIN contraseña. */
    @GetMapping("/{token}")
    public ResponseEntity<VistaPublicaDTO> view(@PathVariable String token) {
        return ResponseEntity.ok(service.consultarPorToken(token, null));
    }

    /** Acceso a un enlace publico CON contraseña. */
    @PostMapping("/{token}/access")
    public ResponseEntity<VistaPublicaDTO> viewWithPassword(@PathVariable String token,
                                                           @RequestBody AccesoEnlaceRequest req) {
        return ResponseEntity.ok(service.consultarPorToken(token, req.getPassword()));
    }
}
