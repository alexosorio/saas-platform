package com.saas.platform.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "detalles_devolucion")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = "devolucion") @EqualsAndHashCode(exclude = "devolucion")
public class DetalleDevolucion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "devolucion_id", nullable = false) private DevolucionVenta devolucion;
    @Column(name = "detalle_orden_id", nullable = false) private Long detalleOrdenId;
    @Column(name = "unidad_id", nullable = false) private Long unidadId;
    @Column(nullable = false) private Integer cantidad;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal monto;
    @Column(nullable = false, length = 20) private String destino;   // disponible | revision | baja
    @Column(name = "bodega_id") private Long bodegaId;
}
