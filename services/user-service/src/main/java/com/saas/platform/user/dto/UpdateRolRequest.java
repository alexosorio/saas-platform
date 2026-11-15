package com.saas.platform.user.dto;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class UpdateRolRequest {
    @Size(max = 50)
    private String nombre;
    
    @Size(max = 255)
    private String descripcion;
    
    private List<CreateRolRequest.PermisoRequest> permisos;
}
