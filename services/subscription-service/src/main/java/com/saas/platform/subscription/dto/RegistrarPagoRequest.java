package com.saas.platform.subscription.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class RegistrarPagoRequest {
    @NotNull @Positive private BigDecimal monto;
    private String moneda;        // default USD
    private Long metodoPagoId;
    private String tipo;          // suscripcion, upgrade, adicional
    private String descripcion;
    private String referenciaExterna;
    // Registro manual
    private String metodo;
    private String banco;
    private String comprobanteUrl;
    private String ultimosDigitos;
    private java.time.LocalDate fechaTransaccion;
}
