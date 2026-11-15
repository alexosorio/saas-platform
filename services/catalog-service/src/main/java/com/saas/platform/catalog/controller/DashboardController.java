package com.saas.platform.catalog.controller;

import com.saas.platform.catalog.dto.DashboardDTO;
import com.saas.platform.catalog.security.UserPrincipal;
import com.saas.platform.catalog.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService service;
    private final com.saas.platform.catalog.service.ParametrosLector parametros;

    /** Indicadores del tablero. dias = período de ingresos; umbral = stock disponible considerado bajo. */
    @GetMapping
    public DashboardDTO resumen(@RequestParam(defaultValue = "30") int dias,
                                @RequestParam(required = false) Integer umbral,
                                @AuthenticationPrincipal UserPrincipal user) {
        int u = umbral != null ? umbral
            : parametros.entero("inventario.umbral_stock_bajo", user.getOrganizacionId(), 5);
        return service.resumen(user.getOrganizacionId(), dias, u);
    }
}
