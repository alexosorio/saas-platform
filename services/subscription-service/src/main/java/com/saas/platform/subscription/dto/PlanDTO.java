package com.saas.platform.subscription.dto;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PlanDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal precioMensual;
    private BigDecimal precioAnual;
    private Integer maxUsuarios;
    private Integer almacenamientoGb;
    private Boolean activo;
    private Integer orden;
    private List<ModuloDTO> modulos;
}
