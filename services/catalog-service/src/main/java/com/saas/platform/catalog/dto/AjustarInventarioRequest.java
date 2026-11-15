package com.saas.platform.catalog.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class AjustarInventarioRequest {
    @NotNull private Long bodegaId;
    @NotNull private Long unidadId;
    @NotNull private Integer cantidad;
    private String tipo;    // entrada, salida, ajuste
    private String motivo;
}
