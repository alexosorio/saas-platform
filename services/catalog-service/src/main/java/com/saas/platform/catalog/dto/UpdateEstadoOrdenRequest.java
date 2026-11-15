package com.saas.platform.catalog.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class UpdateEstadoOrdenRequest {
    @NotNull private String estado;
}
