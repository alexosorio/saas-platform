package com.saas.platform.user.application.commands;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserCommand {
    private String email;
    private String password;
    private String nombre;
    private String apellido;
    private String telefono;
    private Long organizacionId;
    private Long rolId;
}
