package com.saas.platform.catalog.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class CreateBodegaRequest {
    @NotNull private Long sedeId;
    @NotBlank @Size(max = 200) private String nombre;
    @NotBlank @Size(max = 50) private String codigo;
    private String descripcion;
    private BigDecimal capacidadM2;
}
