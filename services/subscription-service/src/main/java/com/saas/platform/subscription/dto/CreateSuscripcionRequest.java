package com.saas.platform.subscription.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class CreateSuscripcionRequest {
    @NotNull private Long planId;
    private String periodo;       // mensual, anual
    private Boolean autoRenovacion;
    private String motivo;        // razon del cambio
}
