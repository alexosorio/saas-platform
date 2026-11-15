package com.saas.platform.user.application.commands;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RegisterUserCommand {
    private String email;
    private String password;
    private String nombre;
    private String apellido;
}
