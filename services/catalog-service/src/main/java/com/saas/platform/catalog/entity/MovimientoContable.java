package com.saas.platform.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "movimientos_contables")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = "asiento") @EqualsAndHashCode(exclude = "asiento")
public class MovimientoContable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "asiento_id", nullable = false) private AsientoContable asiento;
    @Column(name = "cuenta_codigo", nullable = false, length = 20) private String cuentaCodigo;
    @Column(name = "cuenta_nombre", length = 150) private String cuentaNombre;
    @Column(nullable = false, precision = 16, scale = 2) @Builder.Default private BigDecimal debe = BigDecimal.ZERO;
    @Column(nullable = false, precision = 16, scale = 2) @Builder.Default private BigDecimal haber = BigDecimal.ZERO;
    @Column(length = 200) private String tercero;
    @Column(length = 300) private String detalle;
}
