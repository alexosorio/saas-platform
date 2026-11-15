package com.saas.platform.catalog.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class CreatePrecioRequest {
    private Long listaPreciosId;
    @NotNull private Long productoId;
    @NotNull private BigDecimal precio;
    private BigDecimal margenPorcentaje;
}
