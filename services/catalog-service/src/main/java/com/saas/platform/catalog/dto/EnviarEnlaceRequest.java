package com.saas.platform.catalog.dto;
import lombok.Data;
@Data
public class EnviarEnlaceRequest {
    private String destinatario;   // email (para envío por correo)
    private String telefono;       // teléfono (para envío por WhatsApp)
    private String mensaje;        // mensaje opcional que se antepone
}
