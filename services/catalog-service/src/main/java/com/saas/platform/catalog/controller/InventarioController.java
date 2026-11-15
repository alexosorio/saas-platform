package com.saas.platform.catalog.controller;

import com.saas.platform.catalog.dto.AjustarInventarioRequest;
import com.saas.platform.catalog.dto.InventarioDTO;
import com.saas.platform.catalog.dto.MovimientoInventarioDTO;
import com.saas.platform.catalog.security.UserPrincipal;
import com.saas.platform.catalog.service.InventarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class InventarioController {

    private final InventarioService service;

    @GetMapping("/warehouse/{bodegaId}")
    public ResponseEntity<List<InventarioDTO>> getByWarehouse(@PathVariable Long bodegaId) {
        return ResponseEntity.ok(service.getPorBodega(bodegaId));
    }

    /** Existencias de una bodega, paginadas. ?page=0&size=20&q= */
    @GetMapping("/warehouse/{bodegaId}/paginado")
    public com.saas.platform.catalog.dto.PaginaDTO<InventarioDTO> porBodegaPaginado(@PathVariable Long bodegaId, @RequestParam(required = false) String q, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer size) {
        return service.paginarPorBodega(bodegaId, q, page, size);
    }

    @GetMapping("/product/{productoId}")
    public ResponseEntity<List<InventarioDTO>> getByProduct(@PathVariable Long productoId) {
        return ResponseEntity.ok(service.getPorProducto(productoId));
    }

    @GetMapping("/product/{productoId}/stock")
    public ResponseEntity<Map<String, Integer>> getStock(@PathVariable Long productoId) {
        return ResponseEntity.ok(Map.of("stock", service.getStockTotal(productoId)));
    }

    @PostMapping("/adjust")
    public ResponseEntity<InventarioDTO> adjust(@Valid @RequestBody AjustarInventarioRequest req, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.ajustar(req, user.getId()));
    }

    @GetMapping("/movements/{inventarioId}")
    public ResponseEntity<List<MovimientoInventarioDTO>> getMovements(@PathVariable Long inventarioId) {
        return ResponseEntity.ok(service.getMovimientos(inventarioId));
    }
}
