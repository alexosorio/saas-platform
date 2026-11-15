package com.saas.platform.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_precios_producto")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class HistorialPrecioProducto {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "precio_venta_anterior", precision = 15, scale = 2)
    private BigDecimal precioVentaAnterior;
    @Column(name = "precio_venta_nuevo", precision = 15, scale = 2)
    private BigDecimal precioVentaNuevo;

    @Column(name = "precio_compra_anterior", precision = 15, scale = 2)
    private BigDecimal precioCompraAnterior;
    @Column(name = "precio_compra_nuevo", precision = 15, scale = 2)
    private BigDecimal precioCompraNuevo;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(length = 300)
    private String motivo;

    @CreationTimestamp
    @Column(name = "fecha_cambio", updatable = false)
    private LocalDateTime fechaCambio;
}
