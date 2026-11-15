package com.saas.platform.catalog.controller;

import com.saas.platform.catalog.dto.*;
import com.saas.platform.catalog.security.UserPrincipal;
import com.saas.platform.catalog.service.OrdenService;
import com.saas.platform.catalog.service.ScanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class OrdenController {

    private final OrdenService service;
    private final ScanService scanService;
    private final com.saas.platform.catalog.service.DevolucionService devolucionService;

    @GetMapping
    public ResponseEntity<List<OrdenDTO>> getAll(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.getAll(user.getOrganizacionId()));
    }

    /** Órdenes paginadas para la tabla. ?page=0&size=20&estado=&q= */
    @GetMapping("/paginado")
    public com.saas.platform.catalog.dto.PaginaDTO<OrdenDTO> paginado(@RequestParam(required = false) String estado, @RequestParam(required = false) String q,
                                  @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer size, @AuthenticationPrincipal UserPrincipal user) {
        return service.paginado(user.getOrganizacionId(), estado, q, page, size);
    }

    @GetMapping("/by-state/{estado}")
    public ResponseEntity<List<OrdenDTO>> getByState(@PathVariable String estado, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.getPorEstado(user.getOrganizacionId(), estado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenDTO> getById(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.getById(id, user.getOrganizacionId()));
    }

    @PostMapping
    public ResponseEntity<OrdenDTO> create(@Valid @RequestBody CreateOrdenRequest req, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, user.getOrganizacionId(), user.getId()));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<OrdenDTO> confirm(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.confirmar(id, user.getOrganizacionId(), user.getId()));
    }

    /** Pago en proceso: reserva (bloquea) el stock sin sacarlo. */
    @PutMapping("/{id}/reserve")
    public ResponseEntity<OrdenDTO> reserve(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.reservar(id, user.getOrganizacionId(), user.getId()));
    }

    /** Reversa: libera la reserva de una orden con pago en proceso y la vuelve a borrador. */
    @PutMapping("/{id}/reverse")
    public ResponseEntity<OrdenDTO> reverse(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.reversar(id, user.getOrganizacionId(), user.getId()));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrdenDTO> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateEstadoOrdenRequest req, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.actualizarEstado(id, req.getEstado(), user.getOrganizacionId()));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrdenDTO> cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body, @AuthenticationPrincipal UserPrincipal user) {
        String motivo = body != null ? body.getOrDefault("motivo", "Sin motivo") : "Sin motivo";
        return ResponseEntity.ok(service.cancelar(id, motivo, user.getOrganizacionId(), user.getId()));
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<PagoOrdenDTO> registerPayment(@PathVariable Long id, @Valid @RequestBody RegistrarPagoRequest req, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarPago(id, req, user.getOrganizacionId(), user.getId()));
    }

    @GetMapping("/{id}/payments")
    public ResponseEntity<List<PagoOrdenDTO>> getPayments(@PathVariable Long id) {
        return ResponseEntity.ok(service.getPagos(id));
    }

    /**
     * Resuelve un codigo (de barras / SKU / IMEI / codigo unico) a un
     * producto/unidad. Devuelve precio sugerido si se manda listaPreciosId.
     */
    @GetMapping("/scan")
    public ResponseEntity<ScanResultDTO> scan(@RequestParam String codigo,
                                              @RequestParam(required = false) Long listaPreciosId,
                                              @RequestParam(required = false) String color,
                                              @RequestParam(required = false) java.util.List<Long> excluir,
                                              @RequestParam(required = false) Long sedeId,
                                              @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(scanService.scan(codigo, listaPreciosId, color,
            excluir != null ? excluir : java.util.List.of(), sedeId, user.getOrganizacionId()));
    }

    /** Devolución de venta (cualquier motivo) sobre una orden confirmada o completada. */
    @PostMapping("/{id}/devoluciones")
    @org.springframework.security.access.prepost.PreAuthorize("@authz.can('devoluciones','crear')")
    public ResponseEntity<com.saas.platform.catalog.dto.DevolucionDTOs.Devolucion> devolver(
            @PathVariable Long id, @RequestBody com.saas.platform.catalog.dto.DevolucionDTOs.Request req,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
            .body(devolucionService.registrar(id, req, user.getOrganizacionId(), user.getId()));
    }

    @GetMapping("/{id}/devoluciones")
    public java.util.List<com.saas.platform.catalog.dto.DevolucionDTOs.Devolucion> devoluciones(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        return devolucionService.listar(id, user.getOrganizacionId());
    }

    /** Saldo a favor disponible del cliente de la orden (para pagar con nota crédito). */
    @GetMapping("/{id}/saldo-cliente")
    public java.util.Map<String, Object> saldoCliente(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        var orden = service.getById(id, user.getOrganizacionId());
        return java.util.Map.of("saldoAFavor", devolucionService.saldoAFavor(orden.getClienteId()));
    }
}
