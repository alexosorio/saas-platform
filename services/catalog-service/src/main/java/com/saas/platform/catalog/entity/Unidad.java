package com.saas.platform.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "unidades")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"valoresCaracteristicas"})
@ToString(exclude = {"valoresCaracteristicas"})
public class Unidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @Column(name = "codigo_unico", nullable = false, length = 100, unique = true)
    private String codigoUnico;

    /** Código de barras del empaque de ESTA unidad (EAN/UPC/Code128). Puede repetirse entre unidades del mismo modelo. */
    @Column(name = "codigo_barras", length = 64)
    private String codigoBarras;

    @Column(length = 20)
    private String imei1;

    @Column(length = 20)
    private String imei2;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "condicion_producto")
    @Builder.Default
    private Condicion condicion = Condicion.nuevo;

    @Column(name = "nivel_bateria")
    private Integer nivelBateria;

    @Column(name = "precio_compra", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioCompra;

    /** Precio de venta de ESTA unidad (base de las listas de precios con base 'precio de venta'). */
    @Column(name = "precio_venta", precision = 12, scale = 2)
    private BigDecimal precioVenta;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "estado_unidad")
    @Builder.Default
    private EstadoUnidad estado = EstadoUnidad.disponible;

    // Baja de la unidad (nunca se elimina): error_registro | devolucion_proveedor | dano_perdida
    @Column(name = "tipo_baja", length = 30) private String tipoBaja;
    @Column(name = "motivo_baja", columnDefinition = "text") private String motivoBaja;
    @Column(name = "fecha_baja") private java.time.LocalDateTime fechaBaja;
    @Column(name = "usuario_baja") private Long usuarioBaja;

    @Column(columnDefinition = "text")
    private String notas;

    @OneToMany(mappedBy = "unidad", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ValorCaracteristica> valoresCaracteristicas = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Condicion { nuevo, usado }

    // OJO: el schema usa 'dañado' con enie
    public enum EstadoUnidad { disponible, vendido, reservado, dañado, en_reparacion, dado_de_baja }
}
