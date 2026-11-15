package com.saas.platform.subscription.controller;

import com.saas.platform.subscription.dto.PlanDTO;
import com.saas.platform.subscription.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Planes visibles sin iniciar sesión (asistente de registro). /api/public/** ya es público. */
@RestController
@RequiredArgsConstructor
public class PublicPlanController {
    private final PlanService planService;

    @GetMapping("/api/public/plans")
    public List<PlanDTO> planes() { return planService.getActivos(); }
}
