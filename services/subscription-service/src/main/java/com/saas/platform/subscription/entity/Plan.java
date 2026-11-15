package com.saas.platform.subscription.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "planes")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(name = "precio_mensual", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioMensual;

    @Column(name = "precio_anual", precision = 10, scale = 2)
    private BigDecimal precioAnual;

    @Column(name = "max_usuarios")
    @Builder.Default
    private Integer maxUsuarios = 1;

    @Column(name = "almacenamiento_gb")
    @Builder.Default
    private Integer almacenamientoGb = 5;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column
    @Builder.Default
    private Integer orden = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
