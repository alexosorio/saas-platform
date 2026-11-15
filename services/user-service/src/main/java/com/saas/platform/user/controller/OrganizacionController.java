package com.saas.platform.user.controller;

import com.saas.platform.user.security.ContextoActor;
import com.saas.platform.user.service.MarcaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

/** Marca de la organización: nombre y logo (menú de la app, correos y páginas públicas). */
@RestController
@RequiredArgsConstructor
public class OrganizacionController {

    private final MarcaService marca;
    private final ContextoActor actor;

    /** Nombre y logo (data URL) de la organización de quien consulta. */
    @GetMapping("/api/organizacion/marca")
    public Map<String, Object> miMarca() {
        return marca.resumen(actor.organizacionId());
    }

    /** body: { "logo": "data:image/png;base64,..." } — PNG o JPEG, máx. 300 KB. */
    @PutMapping("/api/organizacion/logo")
    @PreAuthorize("@authz.can('configuracion','actualizar')")
    public Map<String, Object> cambiarLogo(@RequestBody Map<String, String> body) {
        marca.guardarLogo(actor.organizacionId(), body.get("logo"));
        return marca.resumen(actor.organizacionId());
    }

    @DeleteMapping("/api/organizacion/logo")
    @PreAuthorize("@authz.can('configuracion','actualizar')")
    public Map<String, Object> quitarLogo() {
        marca.quitarLogo(actor.organizacionId());
        return marca.resumen(actor.organizacionId());
    }

    /**
     * Logo público (lista de precios compartida, servicio de notificaciones).
     * Solo expone la imagen, que es marca pública de la organización.
     */
    @GetMapping("/api/public/organizaciones/{id}/logo")
    public ResponseEntity<byte[]> logoPublico(@PathVariable Long id) {
        return marca.logo(id)
            .map(l -> ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(l.tipo()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(l.datos()))
            .orElse(ResponseEntity.notFound().build());
    }
}
