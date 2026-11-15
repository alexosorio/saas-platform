package com.saas.platform.catalog.controller;

import com.saas.platform.catalog.dto.*;
import com.saas.platform.catalog.security.UserPrincipal;
import com.saas.platform.catalog.service.ListaPreciosService;
import com.saas.platform.catalog.service.PrecioCalculadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lists")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class ListaPreciosController {

    private final ListaPreciosService service;
    private final PrecioCalculadoService precioCalculadoService;

    @GetMapping
    public ResponseEntity<List<ListaPreciosDTO>> getAll(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.getAll(user.getOrganizacionId()));
    }

    @GetMapping("/active")
    public ResponseEntity<List<ListaPreciosDTO>> getActive(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.getActivas(user.getOrganizacionId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListaPreciosDTO> getById(@PathVariable Long id) { return ResponseEntity.ok(service.getById(id)); }

    @PostMapping
    public ResponseEntity<ListaPreciosDTO> create(@Valid @RequestBody CreateListaPreciosRequest req, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, user.getOrganizacionId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ListaPreciosDTO> update(@PathVariable Long id, @Valid @RequestBody CreateListaPreciosRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(MessageResponse.builder().message("Lista eliminada").success(true).build());
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<ListaPreciosDTO> activate(@PathVariable Long id) { return ResponseEntity.ok(service.activar(id)); }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ListaPreciosDTO> deactivate(@PathVariable Long id) { return ResponseEntity.ok(service.desactivar(id)); }

    /**
     * Precios CALCULADOS: todos los productos activos con su precio segun la
     * formula de la lista. Este es el endpoint principal del nuevo modelo.
     */
    @GetMapping("/{id}/computed-prices")
    public ResponseEntity<List<PrecioCalculadoDTO>> computedPrices(@PathVariable Long id,
                                                                  @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(precioCalculadoService.calcularParaLista(id, user.getOrganizacionId()));
    }

    // ── Overrides manuales (tabla `precios`) — excepciones puntuales ──────
    // Si quieres fijar un precio especifico para UN producto en esta lista,
    // distinto de lo que da la formula, usas estos endpoints.

    @GetMapping("/{id}/prices")
    public ResponseEntity<List<PrecioDTO>> getOverrides(@PathVariable Long id) {
        return ResponseEntity.ok(service.getPrecios(id));
    }

    @PostMapping("/{id}/prices")
    public ResponseEntity<PrecioDTO> addOverride(@PathVariable Long id, @Valid @RequestBody CreatePrecioRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarPrecio(id, req));
    }

    @DeleteMapping("/prices/{precioId}")
    public ResponseEntity<MessageResponse> deleteOverride(@PathVariable Long precioId) {
        service.eliminarPrecio(precioId);
        return ResponseEntity.ok(MessageResponse.builder().message("Override eliminado").success(true).build());
    }

    /** Borra TODOS los overrides manuales de la lista para que aplique solo la fórmula. */
    @DeleteMapping("/{id}/prices")
    public ResponseEntity<MessageResponse> clearOverrides(@PathVariable Long id,
                                                          @AuthenticationPrincipal UserPrincipal user) {
        int n = service.limpiarOverrides(id, user.getOrganizacionId());
        return ResponseEntity.ok(MessageResponse.builder()
            .message(n + " precio(s) manual(es) eliminado(s). Ahora aplica la fórmula.").success(true).build());
    }
}
