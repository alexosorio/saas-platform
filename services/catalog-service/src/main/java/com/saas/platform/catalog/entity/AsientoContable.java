package com.saas.platform.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Asiento contable (partida doble). Inmutable en BD: se corrige con otro asiento. */
@Entity @Table(name = "asientos_contables")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AsientoContable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "organizacion_id", nullable = false) private Long organizacionId;
    @Builder.Default private LocalDateTime fecha = LocalDateTime.now();
    @Column(nullable = false, length = 40) private String tipo;
    @Column(nullable = false, length = 60) private String referencia;
    @Column(name = "documento_id") private Long documentoId;
    @Column(columnDefinition = "text") private String descripcion;
    @Column(name = "total_debe", nullable = false, precision = 16, scale = 2) private BigDecimal totalDebe;
    @Column(name = "total_haber", nullable = false, precision = 16, scale = 2) private BigDecimal totalHaber;
    @Column(name = "usuario_id") private Long usuarioId;
    @OneToMany(mappedBy = "asiento", cascade = CascadeType.PERSIST) @Builder.Default
    private List<MovimientoContable> movimientos = new ArrayList<>();
}
