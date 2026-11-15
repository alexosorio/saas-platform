package com.saas.platform.subscription.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HistorialSuscripcionDTO {
    private Long id;
    private Long suscripcionId;
    private Long planAnteriorId;
    private String planAnteriorNombre;
    private Long planNuevoId;
    private String planNuevoNombre;
    private String accion;
    private String motivo;
    private Long usuarioId;
    private BigDecimal montoCambio;
    private LocalDateTime createdAt;
}
