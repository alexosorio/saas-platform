package com.saas.platform.user.application.commands;
import lombok.*;
import java.util.List;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateRolCommand {
    private Long rolId;
    private String nombre;
    private String descripcion;
    private List<CreateRolCommand.PermisoModulo> permisos;
}
