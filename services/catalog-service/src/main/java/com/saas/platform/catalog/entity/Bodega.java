package com.saas.platform.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bodegas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bodega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sede_id", nullable = false)
    private Long sedeId;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(name = "capacidad_m2", precision = 10, scale = 2)
    private BigDecimal capacidadM2;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
