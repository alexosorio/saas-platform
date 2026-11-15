package com.saas.platform.catalog.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PagoProveedorDTO {
    private Long id;
    private BigDecimal monto;
    private String metodo;
    private String banco;
    private String comprobanteUrl;
    private String ultimosDigitos;
    private LocalDate fechaTransaccion;
    private String referencia;
    private String observaciones;
    private LocalDateTime createdAt;
}
