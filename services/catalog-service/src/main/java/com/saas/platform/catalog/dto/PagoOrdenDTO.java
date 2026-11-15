package com.saas.platform.catalog.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PagoOrdenDTO {
    private Long id;
    private Long ordenId;
    private BigDecimal monto;
    private String metodo;
    private String referencia;
    private String observaciones;
    private LocalDateTime fechaPago;
    private Long usuarioRegistro;
    private String banco;
    private java.time.LocalDate fechaTransaccion;
    private String comprobanteUrl;
    private String ultimosDigitos;
}
