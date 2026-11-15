package com.saas.platform.subscription.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "metodos_pago")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MetodoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organizacion_id", nullable = false)
    private Long organizacionId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "tipo_metodo_pago", nullable = false)
    private Tipo tipo;

    @Column(length = 50)
    private String proveedor;

    @Column(name = "ultimos_digitos", length = 4)
    private String ultimosDigitos;

    @Column(length = 255)
    private String token;

    @Column(name = "nombre_titular", length = 200)
    private String nombreTitular;

    @Column(name = "fecha_expiracion")
    private LocalDate fechaExpiracion;

    @Column
    @Builder.Default
    private Boolean predeterminado = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Habeas Data (Ley 1581 de 2012): registro de la autorización de tratamiento de datos
    @Column(name = "autorizacion_datos")
    private Boolean autorizacionDatos;

    @Column(name = "fecha_autorizacion")
    private LocalDateTime fechaAutorizacion;

    @Column(name = "version_politica", length = 20)
    private String versionPolitica;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Tipo { tarjeta_credito, tarjeta_debito, transferencia, paypal, stripe }
}
