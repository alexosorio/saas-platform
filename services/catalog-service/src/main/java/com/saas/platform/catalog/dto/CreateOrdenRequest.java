package com.saas.platform.catalog.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
@Data
public class CreateOrdenRequest {
    @NotNull private Long sedeId;
    @NotNull private Long clienteId;
    @Valid @NotEmpty private List<ItemOrdenRequest> items;
    private BigDecimal descuento;
    private String notas;
}
