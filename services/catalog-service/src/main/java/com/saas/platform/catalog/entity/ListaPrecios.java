package com.saas.platform.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "listas_precios")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ListaPrecios {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organizacion_id", nullable = false)
    private Long organizacionId;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_cliente", columnDefinition = "tipo_cliente", nullable = false)
    private TipoCliente tipoCliente;

    @Column(columnDefinition = "text")
    private String descripcion;

    // ── Formula de la lista ──────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "base_calculo", columnDefinition = "base_calculo_lista", nullable = false)
    @Builder.Default
    private BaseCalculo baseCalculo = BaseCalculo.precio_venta;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_ajuste", columnDefinition = "tipo_ajuste_lista", nullable = false)
    @Builder.Default
    private TipoAjuste tipoAjuste = TipoAjuste.margen_porcentaje;

    @Column(name = "valor_ajuste", precision = 15, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal valorAjuste = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum TipoCliente { mayorista, minorista }
    public enum BaseCalculo { precio_venta, precio_compra }
    public enum TipoAjuste { margen_porcentaje, valor_fijo }
}
