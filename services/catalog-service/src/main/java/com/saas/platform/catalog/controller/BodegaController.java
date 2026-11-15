package com.saas.platform.catalog.controller;
import com.saas.platform.catalog.dto.BodegaDTO;
import com.saas.platform.catalog.dto.CreateBodegaRequest;
import com.saas.platform.catalog.dto.MessageResponse;
import com.saas.platform.catalog.security.UserPrincipal;
import com.saas.platform.catalog.service.BodegaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/warehouses") @RequiredArgsConstructor @CrossOrigin(originPatterns = "*")
public class BodegaController {
    private final BodegaService service;
    @GetMapping
    public ResponseEntity<List<BodegaDTO>> getAll(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.getAll(user.getOrganizacionId()));
    }
    @GetMapping("/by-sede/{sedeId}")
    public ResponseEntity<List<BodegaDTO>> getBySede(@PathVariable Long sedeId, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.getPorSede(sedeId, user.getOrganizacionId()));
    }
    @GetMapping("/{id}")
    public ResponseEntity<BodegaDTO> getById(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.getById(id, user.getOrganizacionId()));
    }
    @PostMapping
    public ResponseEntity<BodegaDTO> create(@Valid @RequestBody CreateBodegaRequest req, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, user.getOrganizacionId()));
    }
    @PutMapping("/{id}")
    public ResponseEntity<BodegaDTO> update(@PathVariable Long id, @Valid @RequestBody CreateBodegaRequest req, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.update(id, req, user.getOrganizacionId()));
    }
    /** Las bodegas no se eliminan: solo se activan o desactivan. */
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<MessageResponse> desactivar(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        service.cambiarEstado(id, user.getOrganizacionId(), false);
        return ResponseEntity.ok(MessageResponse.builder().message("Bodega desactivada").success(true).build());
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<MessageResponse> activar(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        service.cambiarEstado(id, user.getOrganizacionId(), true);
        return ResponseEntity.ok(MessageResponse.builder().message("Bodega activada").success(true).build());
    }
}
