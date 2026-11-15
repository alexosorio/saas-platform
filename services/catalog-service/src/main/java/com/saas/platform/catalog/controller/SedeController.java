package com.saas.platform.catalog.controller;
import com.saas.platform.catalog.dto.CreateSedeRequest;
import com.saas.platform.catalog.dto.MessageResponse;
import com.saas.platform.catalog.dto.SedeDTO;
import com.saas.platform.catalog.security.UserPrincipal;
import com.saas.platform.catalog.service.SedeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/branches") @RequiredArgsConstructor @CrossOrigin(originPatterns = "*")
public class SedeController {
    private final SedeService service;
    @GetMapping
    public ResponseEntity<List<SedeDTO>> getAll(@RequestParam(required = false) Boolean soloActivas, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(Boolean.TRUE.equals(soloActivas) ? service.getActivas(user.getOrganizacionId()) : service.getAll(user.getOrganizacionId()));
    }
    @GetMapping("/{id}")
    public ResponseEntity<SedeDTO> getById(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.getById(id, user.getOrganizacionId()));
    }
    @PostMapping
    public ResponseEntity<SedeDTO> create(@Valid @RequestBody CreateSedeRequest req, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, user.getOrganizacionId()));
    }
    @PutMapping("/{id}")
    public ResponseEntity<SedeDTO> update(@PathVariable Long id, @Valid @RequestBody CreateSedeRequest req, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.update(id, req, user.getOrganizacionId()));
    }
    /** Las sedes no se eliminan: solo se activan o desactivan. */
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<MessageResponse> desactivar(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        service.cambiarEstado(id, user.getOrganizacionId(), false);
        return ResponseEntity.ok(MessageResponse.builder().message("Sede desactivada").success(true).build());
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<MessageResponse> activar(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        service.cambiarEstado(id, user.getOrganizacionId(), true);
        return ResponseEntity.ok(MessageResponse.builder().message("Sede activada").success(true).build());
    }
}
