package com.saas.platform.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Devolución de venta sobre una orden confirmada (por cualquier motivo). */
@Entity @Table(name = "devoluciones_venta")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DevolucionVenta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "organizacion_id", nullable = false) private Long organizacionId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "orden_id", nullable = false) private Orden orden;
    @Column(name = "cliente_id") private Long clienteId;
    @Column(nullable = false, length = 40) private String numero;
    @Column(name = "tipo_motivo", nullable = false, length = 30) private String tipoMotivo;
    @Column(nullable = false, columnDefinition = "text") private String observacion;
    @Column(name = "monto_total", nullable = false, precision = 14, scale = 2) private BigDecimal montoTotal;
    @Column(name = "monto_aplicado_saldo", precision = 14, scale = 2) @Builder.Default private BigDecimal montoAplicadoSaldo = BigDecimal.ZERO;
    @Column(name = "monto_nota_credito", precision = 14, scale = 2) @Builder.Default private BigDecimal montoNotaCredito = BigDecimal.ZERO;
    @Column(length = 20) private String tratamiento;
    @Column(name = "metodo_reembolso", length = 30) private String metodoReembolso;
    @Column(name = "referencia_reembolso", length = 100) private String referenciaReembolso;
    @Column(name = "usuario_id") private Long usuarioId;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @OneToMany(mappedBy = "devolucion", cascade = CascadeType.ALL) @Builder.Default
    private List<DetalleDevolucion> detalles = new ArrayList<>();
}
