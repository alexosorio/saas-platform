package com.saas.platform.catalog.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrdenDTO {
    private Long id;
    private String numeroOrden;
    private Long organizacionId;
    private Long sedeId;
    private String sedeNombre;
    private Long clienteId;
    private String clienteNombre;
    private Long usuarioId;
    private LocalDate fechaOrden;
    private BigDecimal subtotal;
    private BigDecimal descuento;
    private BigDecimal impuestos;
    private BigDecimal total;
    private BigDecimal saldoPendiente;
    private BigDecimal montoDevuelto;
    private String estado;
    private String notas;
    private List<DetalleOrdenDTO> detalles;
    private LocalDateTime createdAt;
}
