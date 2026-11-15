package com.saas.platform.subscription.controller;

import com.saas.platform.subscription.dto.*;
import com.saas.platform.subscription.security.UserPrincipal;
import com.saas.platform.subscription.service.SuscripcionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class SuscripcionController {

    private final SuscripcionService service;

    @GetMapping("/current")
    public ResponseEntity<SuscripcionDTO> getCurrent(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.getActual(user.getOrganizacionId()));
    }

    @GetMapping("/all")
    public ResponseEntity<List<SuscripcionDTO>> getAll(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.getTodas(user.getOrganizacionId()));
    }

    @PostMapping
    public ResponseEntity<SuscripcionDTO> subscribe(@Valid @RequestBody CreateSuscripcionRequest req,
                                                    @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.suscribir(req, user.getOrganizacionId(), user.getId()));
    }

    @PutMapping("/cancel")
    public ResponseEntity<SuscripcionDTO> cancel(@RequestBody(required = false) Map<String, String> body,
                                                 @AuthenticationPrincipal UserPrincipal user) {
        String motivo = body != null ? body.getOrDefault("motivo", "Sin motivo") : "Sin motivo";
        return ResponseEntity.ok(service.cancelar(user.getOrganizacionId(), motivo, user.getId()));
    }

    @PutMapping("/reactivate")
    public ResponseEntity<SuscripcionDTO> reactivate(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.reactivar(user.getOrganizacionId(), user.getId()));
    }

    @PutMapping("/auto-renewal")
    public ResponseEntity<SuscripcionDTO> setAutoRenewal(@RequestBody Map<String, Boolean> body,
                                                         @AuthenticationPrincipal UserPrincipal user) {
        Boolean valor = body.getOrDefault("autoRenovacion", true);
        return ResponseEntity.ok(service.setAutoRenovacion(user.getOrganizacionId(), valor));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<HistorialSuscripcionDTO>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(service.getHistorial(id));
    }

    /** Prueba gratuita de una organización nueva. body: { "planId": 1, "periodo": "mensual" } */
    @PostMapping("/trial")
    public ResponseEntity<SuscripcionDTO> prueba(@RequestBody java.util.Map<String, Object> body,
                                                 @AuthenticationPrincipal UserPrincipal user) {
        Long planId = body.get("planId") != null ? Long.valueOf(String.valueOf(body.get("planId"))) : null;
        if (planId == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
            .body(service.iniciarPrueba(planId, (String) body.get("periodo"), user.getOrganizacionId(), user.getId()));
    }
}
