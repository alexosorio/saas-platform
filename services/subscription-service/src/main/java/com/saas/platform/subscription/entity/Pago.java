package com.saas.platform.subscription.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "suscripcion_id", nullable = false)
    private Long suscripcionId;

    @Column(name = "metodo_pago_id")
    private Long metodoPagoId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(length = 3)
    @Builder.Default
    private String moneda = "USD";

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "estado_pago", nullable = false)
    @Builder.Default
    private Estado estado = Estado.pendiente;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "tipo_pago", nullable = false)
    @Builder.Default
    private Tipo tipo = Tipo.suscripcion;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(name = "referencia_externa", length = 255)
    private String referenciaExterna;

    // Registro manual: método usado y datos/soporte de la transacción
    @Column(length = 50)
    private String metodo;   // transferencia, tarjeta_credito, tarjeta_debito, efectivo

    @Column(length = 100)
    private String banco;

    @Column(name = "comprobante_url", length = 500)
    private String comprobanteUrl;

    @Column(name = "ultimos_digitos", length = 4)
    private String ultimosDigitos;

    @Column(name = "fecha_transaccion")
    private LocalDate fechaTransaccion;

    // Validación del pago por el administrador de la plataforma
    @Column(name = "motivo_rechazo", columnDefinition = "text")
    private String motivoRechazo;

    @Column(name = "validado_por")
    private Long validadoPor;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    @Column(name = "referencia_interna", length = 100, unique = true)
    private String referenciaInterna;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    // 'datos_adicionales' (jsonb) no se mapea aqui: es opcional y dar soporte
    // a jsonb requiere tipos custom. Se deja vacio (la columna es nullable).

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Estado { pendiente, completado, fallido, reembolsado, cancelado }
    public enum Tipo { suscripcion, upgrade, adicional }
}
