package com.saas.platform.catalog.controller;

import com.saas.platform.catalog.dto.CreateMarcaRequest;
import com.saas.platform.catalog.dto.MarcaDTO;
import com.saas.platform.catalog.dto.MessageResponse;
import com.saas.platform.catalog.service.MarcaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class MarcaController {

    private final MarcaService service;

    @GetMapping
    public ResponseEntity<List<MarcaDTO>> getAll() { return ResponseEntity.ok(service.getAll()); }

    @GetMapping("/active")
    public ResponseEntity<List<MarcaDTO>> getActive() { return ResponseEntity.ok(service.getActivas()); }

    @GetMapping("/{id}")
    public ResponseEntity<MarcaDTO> getById(@PathVariable Long id) { return ResponseEntity.ok(service.getById(id)); }

    @PostMapping
    public ResponseEntity<MarcaDTO> create(@Valid @RequestBody CreateMarcaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MarcaDTO> update(@PathVariable Long id, @Valid @RequestBody CreateMarcaRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(MessageResponse.builder().message("Marca eliminada").success(true).build());
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<MarcaDTO> activate(@PathVariable Long id) { return ResponseEntity.ok(service.activar(id)); }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<MarcaDTO> deactivate(@PathVariable Long id) { return ResponseEntity.ok(service.desactivar(id)); }
}
