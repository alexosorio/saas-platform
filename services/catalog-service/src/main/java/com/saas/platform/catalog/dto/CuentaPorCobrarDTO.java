package com.saas.platform.catalog.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CuentaPorCobrarDTO {
    private Long id;
    private Long ordenId;
    private String ordenNumero;
    private Long clienteId;
    private String clienteNombre;
    private BigDecimal monto;
    private BigDecimal saldo;
    private String estado;
    private LocalDateTime createdAt;
    private java.time.LocalDate fechaVencimiento;
    private Integer diasVencida;
}
