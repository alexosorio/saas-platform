package com.saas.platform.catalog.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class CreateProveedorRequest {
    @NotBlank @Size(max = 200) private String nombre;
    @Size(max = 50)  private String identificacion;
    @Size(max = 50)  private String telefono;
    @Size(max = 150) private String email;
    @Size(max = 300) private String direccion;
    @Size(max = 150) private String contacto;
    private String notas;
}
