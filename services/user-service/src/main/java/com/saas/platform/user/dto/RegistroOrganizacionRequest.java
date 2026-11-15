package com.saas.platform.user.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/** Registro de una organización nueva junto con su usuario propietario (asistente de registro). */
@Data
public class RegistroOrganizacionRequest {
    @NotBlank(message = "El nombre de la organización es obligatorio") @Size(max = 200)
    private String organizacion;
    private String telefono;
    @NotBlank(message = "El nombre es obligatorio") private String nombre;
    private String apellido;
    @NotBlank @Email(message = "Correo inválido") private String email;
    @NotBlank @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres") private String password;
    @AssertTrue(message = "Debes aceptar los términos y la política de tratamiento de datos")
    private Boolean aceptaTerminos;
    /** Opcional: logo como data URL PNG/JPEG (el asistente convierte SVG a PNG). */
    private String logo;
}
