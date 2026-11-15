package com.saas.platform.catalog.controller;

import com.saas.platform.catalog.dto.ClienteDTO;
import com.saas.platform.catalog.dto.CreateClienteRequest;
import com.saas.platform.catalog.dto.MessageResponse;
import com.saas.platform.catalog.security.UserPrincipal;
import com.saas.platform.catalog.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class ClienteController {

    private final ClienteService service;

    @GetMapping
    public ResponseEntity<List<ClienteDTO>> getAll(@RequestParam(required = false) String q,
                                                   @AuthenticationPrincipal UserPrincipal user) {
        if (q != null && !q.isBlank()) return ResponseEntity.ok(service.buscar(user.getOrganizacionId(), q));
        return ResponseEntity.ok(service.getAll(user.getOrganizacionId()));
    }

    /** Clientes paginados para la tabla. ?page=0&size=20&q= */
    @GetMapping("/paginado")
    public com.saas.platform.catalog.dto.PaginaDTO<ClienteDTO> paginado(@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer size,
                                    @AuthenticationPrincipal UserPrincipal user) {
        return service.paginado(user.getOrganizacionId(), q, page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteDTO> getById(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.getById(id, user.getOrganizacionId()));
    }

    @PostMapping
    public ResponseEntity<ClienteDTO> create(@Valid @RequestBody CreateClienteRequest req, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, user.getOrganizacionId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteDTO> update(@PathVariable Long id, @Valid @RequestBody CreateClienteRequest req, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.update(id, req, user.getOrganizacionId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        service.delete(id, user.getOrganizacionId());
        return ResponseEntity.ok(MessageResponse.builder().message("Cliente desactivado").success(true).build());
    }
}
