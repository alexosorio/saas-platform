package com.saas.platform.catalog.controller;

import com.saas.platform.catalog.dto.CreateProveedorRequest;
import com.saas.platform.catalog.dto.MessageResponse;
import com.saas.platform.catalog.dto.ProveedorDTO;
import com.saas.platform.catalog.security.UserPrincipal;
import com.saas.platform.catalog.service.ProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class ProveedorController {

    private final ProveedorService service;

    @GetMapping
    public ResponseEntity<List<ProveedorDTO>> getAll(@RequestParam(required = false) Boolean soloActivos,
                                                     @RequestParam(required = false) String q,
                                                     @AuthenticationPrincipal UserPrincipal user) {
        if (q != null && !q.isBlank()) return ResponseEntity.ok(service.buscar(user.getOrganizacionId(), q));
        return ResponseEntity.ok(Boolean.TRUE.equals(soloActivos)
            ? service.getActivos(user.getOrganizacionId())
            : service.getAll(user.getOrganizacionId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProveedorDTO> getById(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.getById(id, user.getOrganizacionId()));
    }

    @PostMapping
    public ResponseEntity<ProveedorDTO> create(@Valid @RequestBody CreateProveedorRequest req,
                                              @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, user.getOrganizacionId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProveedorDTO> update(@PathVariable Long id, @Valid @RequestBody CreateProveedorRequest req,
                                              @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.update(id, req, user.getOrganizacionId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        service.delete(id, user.getOrganizacionId());
        return ResponseEntity.ok(MessageResponse.builder().message("Proveedor desactivado").success(true).build());
    }
}
