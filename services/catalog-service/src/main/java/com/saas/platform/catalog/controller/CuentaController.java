package com.saas.platform.catalog.controller;

import com.saas.platform.catalog.dto.*;
import com.saas.platform.catalog.security.UserPrincipal;
import com.saas.platform.catalog.service.CuentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService service;

    @GetMapping("/por-pagar")
    public List<CuentaPorPagarDTO> porPagar(@RequestParam(required = false) String estado,
                                            @RequestParam(required = false) Long proveedorId,
                                            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate desde,
                                            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate hasta,
                                            @RequestParam(required = false, defaultValue = "creacion") String campoFecha,
                                            @RequestParam(required = false) Boolean soloVencidas,
                                            @AuthenticationPrincipal UserPrincipal user) {
        return service.listarPorPagar(user.getOrganizacionId(), CuentaService.FiltroCuentas.builder()
            .estado(estado).proveedorId(proveedorId).desde(desde).hasta(hasta)
            .campoFecha(campoFecha).soloVencidas(soloVencidas).build());
    }

    /** Cuentas por pagar paginadas (mismos filtros); resumen.saldoPendiente es el total del filtro. */
    @GetMapping("/por-pagar/paginado")
    public com.saas.platform.catalog.dto.PaginaDTO<CuentaPorPagarDTO> porPagarPaginado(@RequestParam(required = false) String estado,
                                            @RequestParam(required = false) Long proveedorId,
                                            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate desde,
                                            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate hasta,
                                            @RequestParam(required = false, defaultValue = "creacion") String campoFecha,
                                            @RequestParam(required = false) Boolean soloVencidas,
                                            @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer size, @AuthenticationPrincipal UserPrincipal user) {
        return service.paginarPorPagar(user.getOrganizacionId(), CuentaService.FiltroCuentas.builder()
            .estado(estado).proveedorId(proveedorId).desde(desde).hasta(hasta)
            .campoFecha(campoFecha).soloVencidas(soloVencidas).build(), page, size);
    }

    /** Cuentas por cobrar paginadas (mismos filtros); resumen.saldoPendiente es el total del filtro. */
    @GetMapping("/por-cobrar/paginado")
    public com.saas.platform.catalog.dto.PaginaDTO<CuentaPorCobrarDTO> porCobrarPaginado(@RequestParam(required = false) String estado,
                                            @RequestParam(required = false) Long clienteId,
                                            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate desde,
                                            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate hasta,
                                            @RequestParam(required = false, defaultValue = "creacion") String campoFecha,
                                            @RequestParam(required = false) Boolean soloVencidas,
                                            @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer size, @AuthenticationPrincipal UserPrincipal user) {
        return service.paginarPorCobrar(user.getOrganizacionId(), CuentaService.FiltroCuentas.builder()
            .estado(estado).clienteId(clienteId).desde(desde).hasta(hasta)
            .campoFecha(campoFecha).soloVencidas(soloVencidas).build(), page, size);
    }

    @GetMapping("/por-cobrar")
    public List<CuentaPorCobrarDTO> porCobrar(@RequestParam(required = false) String estado,
                                            @RequestParam(required = false) Long clienteId,
                                            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate desde,
                                            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate hasta,
                                            @RequestParam(required = false, defaultValue = "creacion") String campoFecha,
                                            @RequestParam(required = false) Boolean soloVencidas,
                                              @AuthenticationPrincipal UserPrincipal user) {
        return service.listarPorCobrar(user.getOrganizacionId(), CuentaService.FiltroCuentas.builder()
            .estado(estado).clienteId(clienteId).desde(desde).hasta(hasta)
            .campoFecha(campoFecha).soloVencidas(soloVencidas).build());
    }

    @GetMapping("/por-pagar/{id}/pagos")
    public List<PagoProveedorDTO> pagos(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        return service.pagosDeCuenta(id, user.getOrganizacionId());
    }

    @PostMapping("/por-pagar/{id}/pagos")
    public ResponseEntity<PagoProveedorDTO> registrarPago(@PathVariable Long id,
                                                          @RequestBody RegistrarPagoProveedorRequest req,
                                                          @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.registrarPagoProveedor(id, user.getOrganizacionId(), user.getId(), req));
    }
}
