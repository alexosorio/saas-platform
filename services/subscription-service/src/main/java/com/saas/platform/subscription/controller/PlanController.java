package com.saas.platform.subscription.controller;

import com.saas.platform.subscription.dto.PlanDTO;
import com.saas.platform.subscription.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class PlanController {

    private final PlanService service;

    @GetMapping public ResponseEntity<List<PlanDTO>> getAll() { return ResponseEntity.ok(service.getAll()); }

    @GetMapping("/active")
    public ResponseEntity<List<PlanDTO>> getActive() { return ResponseEntity.ok(service.getActivos()); }

    @GetMapping("/{id}")
    public ResponseEntity<PlanDTO> getById(@PathVariable Long id) { return ResponseEntity.ok(service.getById(id)); }
}
