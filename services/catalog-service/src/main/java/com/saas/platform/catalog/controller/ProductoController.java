package com.saas.platform.catalog.controller;

import com.saas.platform.catalog.dto.*;
import com.saas.platform.catalog.security.UserPrincipal;
import com.saas.platform.catalog.service.ProductoBulkService;
import com.saas.platform.catalog.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class ProductoController {

    private final ProductoService service;
    private final ProductoBulkService bulkService;

    @GetMapping
    public ResponseEntity<List<ProductoDTO>> getAll(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.getAll(user.getOrganizacionId()));
    }

    /** Productos paginados para la tabla. ?page=0&size=20&q=&activo= */
    @GetMapping("/paginado")
    public com.saas.platform.catalog.dto.PaginaDTO<ProductoDTO> paginado(@RequestParam(required = false) String q, @RequestParam(required = false) Boolean activo,
                                     @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer size, @AuthenticationPrincipal UserPrincipal user) {
        return service.paginado(user.getOrganizacionId(), q, activo, page, size);
    }

    @GetMapping("/active")
    public ResponseEntity<List<ProductoDTO>> getActive(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.getActivos(user.getOrganizacionId()));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductoDTO>> search(@RequestParam String q, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.search(user.getOrganizacionId(), q));
    }

    @GetMapping("/by-barcode/{codigo}")
    public ResponseEntity<ProductoDTO> byBarcode(@PathVariable String codigo, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.getByBarcode(user.getOrganizacionId(), codigo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoDTO> getById(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.getById(id, user.getOrganizacionId()));
    }

    @GetMapping("/{id}/price-history")
    public ResponseEntity<List<HistorialPrecioDTO>> priceHistory(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.getHistorialPrecios(id, user.getOrganizacionId()));
    }

    @PostMapping
    public ResponseEntity<ProductoDTO> create(@Valid @RequestBody CreateProductoRequest req, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, user.getOrganizacionId(), user.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoDTO> update(@PathVariable Long id, @Valid @RequestBody UpdateProductoRequest req, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.update(id, req, user.getOrganizacionId(), user.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        service.delete(id, user.getOrganizacionId());
        return ResponseEntity.ok(MessageResponse.builder().message("Producto eliminado").success(true).build());
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<ProductoDTO> activate(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.activar(id, user.getOrganizacionId()));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ProductoDTO> deactivate(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.desactivar(id, user.getOrganizacionId()));
    }

    /** Carga masiva de productos desde un archivo CSV. */
    @PostMapping("/bulk")
    public ResponseEntity<CargaMasivaResultDTO> bulkUpload(@RequestParam("file") MultipartFile file,
                                                          @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(bulkService.cargar(file, user.getOrganizacionId(), user.getId()));
    }
}
