package com.saas.platform.subscription.controller;

import com.saas.platform.subscription.dto.ModuloDTO;
import com.saas.platform.subscription.service.ModuloService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class ModuloController {
    private final ModuloService service;

    @GetMapping public ResponseEntity<List<ModuloDTO>> getActive() {
        return ResponseEntity.ok(service.getActivos());
    }
}
