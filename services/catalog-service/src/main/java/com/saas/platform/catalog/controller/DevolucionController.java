package com.saas.platform.catalog.controller;

import com.saas.platform.catalog.dto.DevolucionDTOs;
import com.saas.platform.catalog.security.UserPrincipal;
import com.saas.platform.catalog.service.DevolucionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Módulo de devoluciones: búsqueda por factura o cliente, registro, historial y asiento contable. */
@RestController
@RequestMapping("/api/devoluciones")
@RequiredArgsConstructor
public class DevolucionController {
    private final DevolucionService service;

    @GetMapping
    public List<DevolucionDTOs.Devolucion> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String q, @AuthenticationPrincipal UserPrincipal user) {
        return service.listarTodas(user.getOrganizacionId(), desde, hasta, q);
    }

    /** Devoluciones paginadas. ?page=0&size=20&desde=&hasta=&q= */
    @GetMapping("/paginado")
    public com.saas.platform.catalog.dto.PaginaDTO<DevolucionDTOs.Devolucion> paginado(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String q, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer size, @AuthenticationPrincipal UserPrincipal user) {
        return service.paginado(user.getOrganizacionId(), desde, hasta, q, page, size);
    }

    @GetMapping("/{id}")
    public DevolucionDTOs.Devolucion detalle(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        return service.detalle(id, user.getOrganizacionId());
    }

    /** Facturas a devolver: por número o por nombre/documento del cliente. */
    @GetMapping("/facturas")
    public List<Map<String, Object>> facturas(@RequestParam String q, @AuthenticationPrincipal UserPrincipal user) {
        return service.buscarFacturas(q, user.getOrganizacionId());
    }

    @PostMapping("/factura/{ordenId}")
    @PreAuthorize("@authz.can('devoluciones','crear')")
    public ResponseEntity<DevolucionDTOs.Devolucion> registrar(@PathVariable Long ordenId, @RequestBody DevolucionDTOs.Request req,
                                                               @AuthenticationPrincipal UserPrincipal user) {
        DevolucionDTOs.Devolucion d = service.registrar(ordenId, req, user.getOrganizacionId(), user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.detalle(d.getId(), user.getOrganizacionId()));
    }
}
