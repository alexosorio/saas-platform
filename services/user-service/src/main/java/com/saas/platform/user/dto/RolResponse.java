package com.saas.platform.user.dto;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RolResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean esSistema;
    private Long organizacionId;     // null = rol del sistema
    private Boolean editable;        // ¿quien consulta puede modificarlo?
    private Boolean activo;
    private List<PermisoDTO> permisos;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PermisoDTO {
        private Long id;
        private Long moduloId;
        private String moduloCodigo;
        private String moduloNombre;
        private Boolean crear;
        private Boolean leer;
        private Boolean actualizar;
        private Boolean eliminar;
    }
}
