package com.saas.platform.catalog.controller;

import com.saas.platform.catalog.dto.CategoriaDTO;
import com.saas.platform.catalog.dto.CreateCategoriaRequest;
import com.saas.platform.catalog.dto.MessageResponse;
import com.saas.platform.catalog.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class CategoriaController {

    private final CategoriaService service;

    @GetMapping
    public ResponseEntity<List<CategoriaDTO>> getAll() { return ResponseEntity.ok(service.getAll()); }

    @GetMapping("/active")
    public ResponseEntity<List<CategoriaDTO>> getActive() { return ResponseEntity.ok(service.getActivas()); }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaDTO> getById(@PathVariable Long id) { return ResponseEntity.ok(service.getById(id)); }

    @PostMapping
    public ResponseEntity<CategoriaDTO> create(@Valid @RequestBody CreateCategoriaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaDTO> update(@PathVariable Long id, @Valid @RequestBody CreateCategoriaRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(MessageResponse.builder().message("Categoria eliminada").success(true).build());
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<CategoriaDTO> activate(@PathVariable Long id) { return ResponseEntity.ok(service.activar(id)); }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<CategoriaDTO> deactivate(@PathVariable Long id) { return ResponseEntity.ok(service.desactivar(id)); }
}
