package com.saas.platform.subscription.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "modulos")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Modulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 50, unique = true)
    private String codigo;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(length = 255)
    private String ruta;

    @Column(length = 100)
    private String icono;

    @Column
    @Builder.Default
    private Integer orden = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
