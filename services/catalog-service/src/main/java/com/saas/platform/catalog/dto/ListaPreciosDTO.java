package com.saas.platform.catalog.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ListaPreciosDTO {
    private Long id;
    private Long organizacionId;
    private String nombre;
    private String tipoCliente;
    private String descripcion;
    private String baseCalculo;     // precio_venta | precio_compra
    private String tipoAjuste;      // margen_porcentaje | valor_fijo
    private BigDecimal valorAjuste;
    private Boolean activa;
    private LocalDateTime createdAt;
}
