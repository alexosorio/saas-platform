package com.saas.platform.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Nota sobre una cuenta por pagar. 'credito' reduce lo que se le debe al proveedor. */
@Entity @Table(name = "notas_cuenta_pagar")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class NotaCuentaPagar {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_por_pagar_id", nullable = false)
    private CuentaPorPagar cuentaPorPagar;

    @Column(name = "unidad_id") private Long unidadId;
    @Column(nullable = false, length = 20) @Builder.Default private String tipo = "credito";
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal monto;
    @Column(nullable = false, columnDefinition = "text") private String motivo;
    @Column(name = "usuario_id") private Long usuarioId;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
