package com.saas.platform.catalog.controller;

import com.saas.platform.catalog.dto.CaracteristicaDTO;
import com.saas.platform.catalog.dto.CreateCaracteristicaRequest;
import com.saas.platform.catalog.dto.MessageResponse;
import com.saas.platform.catalog.service.CaracteristicaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/characteristics")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class CaracteristicaController {

    private final CaracteristicaService service;

    @GetMapping
    public ResponseEntity<List<CaracteristicaDTO>> getAll() { return ResponseEntity.ok(service.getAll()); }

    @GetMapping("/by-type/{aplicableA}")
    public ResponseEntity<List<CaracteristicaDTO>> getByType(@PathVariable String aplicableA) {
        return ResponseEntity.ok(service.getPorTipo(aplicableA));
    }

    @PostMapping
    public ResponseEntity<CaracteristicaDTO> create(@Valid @RequestBody CreateCaracteristicaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CaracteristicaDTO> update(@PathVariable Long id, @Valid @RequestBody CreateCaracteristicaRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(MessageResponse.builder().message("Caracteristica eliminada").success(true).build());
    }
}
