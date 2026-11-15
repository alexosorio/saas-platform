package com.saas.platform.subscription.dto;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SuscripcionDTO {
    private Long id;
    private Long organizacionId;
    private Long planId;
    private String planNombre;
    private String estado;
    private String periodo;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Boolean autoRenovacion;
    private LocalDateTime createdAt;
    private Long diasParaVencer;   // negativo si ya pasó la fecha de fin
    private Integer diasAviso;     // desde cuántos días antes se avisa (para el aviso en pantalla)
}
