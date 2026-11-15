package com.saas.platform.catalog.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BodegaDTO {
    private Long id; private Long sedeId; private String sedeNombre;
    private String nombre; private String codigo; private String descripcion;
    private BigDecimal capacidadM2; private Boolean activo; private LocalDateTime createdAt;
}
