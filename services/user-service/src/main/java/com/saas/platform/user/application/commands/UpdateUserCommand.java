package com.saas.platform.user.application.commands;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateUserCommand {
    private Long userId;
    private String nombre;
    private String apellido;
    private String telefono;
    private String avatarUrl;
    private String idioma;
    private String zonaHoraria;
}
