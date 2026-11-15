package com.saas.platform.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos_orden")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"orden"})
@ToString(exclude = {"orden"})
public class PagoOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden orden;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "metodo_pago_orden", nullable = false)
    private MetodoPago metodo;

    @Column(length = 100)
    private String referencia;

    @Column(columnDefinition = "text")
    private String observaciones;

    // Registro manual: datos de la transacción y soporte
    @Column(length = 100)
    private String banco;

    @Column(name = "fecha_transaccion")
    private java.time.LocalDate fechaTransaccion;

    @Column(name = "comprobante_url", length = 500)
    private String comprobanteUrl;

    @Column(name = "ultimos_digitos", length = 4)
    private String ultimosDigitos;

    @CreationTimestamp
    @Column(name = "fecha_pago", updatable = false)
    private LocalDateTime fechaPago;

    @Column(name = "usuario_registro", nullable = false)
    private Long usuarioRegistro;

    public enum MetodoPago { efectivo, tarjeta, transferencia, credito, nota_credito }
}
