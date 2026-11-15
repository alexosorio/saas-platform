package com.saas.platform.user.dto;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class CreateRolRequest {
    @NotBlank(message = "El nombre es requerido")
    @Size(max = 50)
    private String nombre;
    
    @Size(max = 255)
    private String descripcion;
    
    private List<PermisoRequest> permisos;
    
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PermisoRequest {
        @NotNull
        private Long moduloId;
        private String moduloCodigo;
        private String moduloNombre;
        private Boolean crear = false;
        private Boolean leer = false;
        private Boolean actualizar = false;
        private Boolean eliminar = false;
    }
}
