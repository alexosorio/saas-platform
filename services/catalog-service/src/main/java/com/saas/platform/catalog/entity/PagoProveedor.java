package com.saas.platform.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "pagos_proveedor")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PagoProveedor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_por_pagar_id", nullable = false)
    private CuentaPorPagar cuentaPorPagar;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    @Column(length = 50)
    private String metodo;              // transferencia | tarjeta | efectivo
    @Column(length = 100)
    private String banco;
    @Column(name = "comprobante_url", length = 500)
    private String comprobanteUrl;      // soporte de la transferencia al proveedor
    @Column(name = "ultimos_digitos", length = 4)
    private String ultimosDigitos;
    @Column(name = "fecha_transaccion")
    private LocalDate fechaTransaccion;
    @Column(length = 255)
    private String referencia;
    @Column(columnDefinition = "text")
    private String observaciones;

    @Column(name = "usuario_registro")
    private Long usuarioRegistro;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
