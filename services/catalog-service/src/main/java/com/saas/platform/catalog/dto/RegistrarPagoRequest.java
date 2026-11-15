package com.saas.platform.catalog.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class RegistrarPagoRequest {
    @NotNull @Positive private BigDecimal monto;
    @NotNull private String metodo;   // efectivo, tarjeta, transferencia, credito
    private String referencia;
    private String observaciones;
    // Registro manual
    private String banco;
    private java.time.LocalDate fechaTransaccion;
    private String comprobanteUrl;
    private String ultimosDigitos;
}
