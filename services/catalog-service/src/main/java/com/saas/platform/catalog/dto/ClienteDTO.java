package com.saas.platform.catalog.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ClienteDTO {
    private Long id;
    private Long organizacionId;
    private String tipo;
    private String nombre;
    private String documento;
    private String email;
    private String telefono;
    private String direccion;
    private Long ciudadId;
    private BigDecimal limiteCredito;
    private Boolean activo;
    private LocalDateTime createdAt;
}
