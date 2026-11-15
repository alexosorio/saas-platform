package com.saas.platform.catalog.controller;

import com.saas.platform.catalog.dto.CreateUnidadRequest;
import com.saas.platform.catalog.dto.MessageResponse;
import com.saas.platform.catalog.dto.UnidadDTO;
import com.saas.platform.catalog.service.UnidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.saas.platform.catalog.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/units")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class UnidadController {

    private final UnidadService service;
    private final com.saas.platform.catalog.service.BajaUnidadService bajaService;

    @GetMapping("/product/{productoId}")
    public ResponseEntity<List<UnidadDTO>> getByProduct(@PathVariable Long productoId) {
        return ResponseEntity.ok(service.getPorProducto(productoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnidadDTO> getById(@PathVariable Long id) { return ResponseEntity.ok(service.getById(id)); }

    @GetMapping("/imei/{imei}")
    public ResponseEntity<UnidadDTO> searchByImei(@PathVariable String imei) {
        return ResponseEntity.ok(service.buscarPorImei(imei));
    }

    @PostMapping
    public ResponseEntity<UnidadDTO> create(@Valid @RequestBody CreateUnidadRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    /** Las unidades no se eliminan: se dan de baja con motivo. body: { "tipo": "error_registro", "motivo": "..." } */
    @PostMapping("/{id}/baja")
    @PreAuthorize("@authz.can('inventario','eliminar')")
    public ResponseEntity<UnidadDTO> darDeBaja(@PathVariable Long id, @RequestBody java.util.Map<String, String> body,
                                               @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(bajaService.darDeBaja(id, body.get("tipo"), body.get("motivo"), user.getOrganizacionId()));
    }

    /** Asigna o cambia el código de barras de la unidad. body: { "codigoBarras": "7701234567890" } (vacío = quitar) */
    @PutMapping("/{id}/codigo-barras")
    @PreAuthorize("@authz.can('inventario','actualizar')")
    public ResponseEntity<UnidadDTO> cambiarCodigoBarras(@PathVariable Long id, @RequestBody java.util.Map<String, String> body,
                                                         @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.cambiarCodigoBarras(id, body.get("codigoBarras"), user.getOrganizacionId()));
    }

    @PutMapping("/{id}/state/{estado}")
    public ResponseEntity<UnidadDTO> changeState(@PathVariable Long id, @PathVariable String estado) {
        return ResponseEntity.ok(service.cambiarEstado(id, estado));
    }
}
