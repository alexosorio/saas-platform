package com.saas.platform.catalog.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class ItemOrdenRequest {
    @NotNull private Long unidadId;
    @NotNull @Positive private Integer cantidad;
    @NotNull private BigDecimal precioUnitario;
    private BigDecimal descuentoLinea;
}
