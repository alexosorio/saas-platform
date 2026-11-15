package com.saas.platform.user.application.commands;
import lombok.*;
import java.util.List;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateRolCommand {
    private String nombre;
    private String descripcion;
    private List<PermisoModulo> permisos;
    
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PermisoModulo {
        private Long moduloId;
        private String moduloCodigo;
        private String moduloNombre;
        private Boolean crear;
        private Boolean leer;
        private Boolean actualizar;
        private Boolean eliminar;
    }
}
