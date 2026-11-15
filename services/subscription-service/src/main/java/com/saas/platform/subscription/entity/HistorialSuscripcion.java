package com.saas.platform.subscription.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_suscripciones")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class HistorialSuscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "suscripcion_id", nullable = false)
    private Long suscripcionId;

    @Column(name = "plan_anterior_id")
    private Long planAnteriorId;

    @Column(name = "plan_nuevo_id")
    private Long planNuevoId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "accion_suscripcion", nullable = false)
    private Accion accion;

    @Column(columnDefinition = "text")
    private String motivo;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "monto_cambio", precision = 10, scale = 2)
    private BigDecimal montoCambio;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Accion { creacion, upgrade, downgrade, cancelacion, renovacion, reactivacion, suspension }
}
