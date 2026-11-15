package com.saas.platform.catalog.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class CreateMarcaRequest {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100) private String nombre;
    @NotBlank(message = "El codigo es obligatorio")
    @Size(max = 50) private String codigo;
    private String logoUrl;
}
