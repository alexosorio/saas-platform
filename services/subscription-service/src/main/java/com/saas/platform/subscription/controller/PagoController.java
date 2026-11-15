package com.saas.platform.subscription.controller;

import com.saas.platform.subscription.dto.PagoDTO;
import com.saas.platform.subscription.dto.RegistrarPagoRequest;
import com.saas.platform.subscription.security.UserPrincipal;
import com.saas.platform.subscription.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class PagoController {

    private final PagoService service;

    @GetMapping
    public ResponseEntity<List<PagoDTO>> getAll(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.getPorOrganizacion(user.getOrganizacionId()));
    }

    @GetMapping("/subscription/{suscripcionId}")
    public ResponseEntity<List<PagoDTO>> getBySubscription(@PathVariable Long suscripcionId) {
        return ResponseEntity.ok(service.getPorSuscripcion(suscripcionId));
    }

    @PostMapping
    public ResponseEntity<PagoDTO> register(@Valid @RequestBody RegistrarPagoRequest req, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(req, user.getOrganizacionId()));
    }
}
