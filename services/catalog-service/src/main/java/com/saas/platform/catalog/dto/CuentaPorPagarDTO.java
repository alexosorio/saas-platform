package com.saas.platform.catalog.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CuentaPorPagarDTO {
    private Long id;
    private Long unidadId;
    private String unidadCodigo;
    private String productoNombre;
    private Long proveedorId;
    private String proveedorNombre;
    private BigDecimal monto;
    private BigDecimal saldo;
    private String estado;
    private String descripcion;
    private LocalDateTime createdAt;
    private java.time.LocalDate fechaVencimiento;
    private Integer diasVencida;
    private java.util.List<java.util.Map<String, Object>> notas;   // notas crédito aplicadas
}
