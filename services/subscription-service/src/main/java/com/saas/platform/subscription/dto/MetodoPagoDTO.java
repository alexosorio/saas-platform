package com.saas.platform.subscription.dto;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MetodoPagoDTO {
    private Long id;
    private Long organizacionId;
    private String tipo;
    private String proveedor;
    private String ultimosDigitos;
    private String nombreTitular;
    private LocalDate fechaExpiracion;
    private Boolean predeterminado;
    private Boolean activo;
    private LocalDateTime createdAt;
}
