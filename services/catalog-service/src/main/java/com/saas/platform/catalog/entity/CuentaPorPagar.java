package com.saas.platform.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "cuentas_por_pagar")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CuentaPorPagar {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organizacion_id", nullable = false)
    private Long organizacionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidad_id")
    private Unidad unidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal saldo = BigDecimal.ZERO;

    @Column(length = 20, nullable = false)
    @Builder.Default
    private String estado = "pendiente";

    /** Fecha límite de pago (creación + plazo configurado). Base del análisis de cartera vencida. */
    @Column(name = "fecha_vencimiento")
    private java.time.LocalDate fechaVencimiento;   // pendiente | parcial | pagada

    @Column(columnDefinition = "text")
    private String descripcion;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
