package com.saas.platform.catalog.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class CreateSedeRequest {
    @NotBlank(message = "El nombre de la sede es obligatorio") @Size(max = 200) private String nombre;
    @NotNull(message = "La ciudad es obligatoria") private Long ciudadId;
    private String direccion;
    @Size(max = 20) private String telefono;
    @Size(max = 255) private String email;
}
