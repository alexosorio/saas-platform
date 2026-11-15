package com.saas.platform.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Nota crédito a favor del cliente (excedente pagado en una devolución). */
@Entity @Table(name = "notas_credito_cliente")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class NotaCreditoCliente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "organizacion_id", nullable = false) private Long organizacionId;
    @Column(name = "cliente_id") private Long clienteId;
    @Column(name = "devolucion_id") private Long devolucionId;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal monto;
    @Column(name = "saldo_disponible", nullable = false, precision = 14, scale = 2) private BigDecimal saldoDisponible;
    @Column(nullable = false, length = 20) private String tratamiento;   // reembolso | saldo_a_favor
    @Column(nullable = false, length = 20) private String estado;        // reembolsada | disponible | aplicada
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
