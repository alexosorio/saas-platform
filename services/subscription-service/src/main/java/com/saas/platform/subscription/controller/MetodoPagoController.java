package com.saas.platform.subscription.controller;

import com.saas.platform.subscription.dto.CreateMetodoPagoRequest;
import com.saas.platform.subscription.dto.MessageResponse;
import com.saas.platform.subscription.dto.MetodoPagoDTO;
import com.saas.platform.subscription.security.UserPrincipal;
import com.saas.platform.subscription.service.MetodoPagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class MetodoPagoController {

    private final MetodoPagoService service;

    @GetMapping
    public ResponseEntity<List<MetodoPagoDTO>> getAll(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.getAll(user.getOrganizacionId()));
    }

    @PostMapping
    public ResponseEntity<MetodoPagoDTO> create(@Valid @RequestBody CreateMetodoPagoRequest req,
                                                @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, user.getOrganizacionId()));
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<MetodoPagoDTO> markDefault(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.marcarPredeterminado(id, user.getOrganizacionId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        service.delete(id, user.getOrganizacionId());
        return ResponseEntity.ok(MessageResponse.builder().message("Metodo de pago eliminado").success(true).build());
    }
}
