package com.saas.platform.subscription.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PagoDTO {
    private Long id;
    private Long suscripcionId;
    private Long metodoPagoId;
    private BigDecimal monto;
    private String moneda;
    private String estado;
    private String tipo;
    private String descripcion;
    private String referenciaExterna;
    private String referenciaInterna;
    private LocalDate fechaPago;
    private LocalDate fechaVencimiento;
    private String metodo;
    private String banco;
    private String comprobanteUrl;
    private String ultimosDigitos;
    private LocalDate fechaTransaccion;
    private String motivoRechazo;
    private LocalDateTime fechaValidacion;
    private LocalDateTime createdAt;
}
