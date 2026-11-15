package com.saas.platform.user.entity;

import jakarta.persistence.*;
import lombok.*;

/** Módulo funcional de la plataforma (tabla compartida 'modulos'). Solo lectura desde el user-service. */
@Entity @Table(name = "modulos")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Modulo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100) private String nombre;
    @Column(nullable = false, length = 50, unique = true) private String codigo;
    @Column(columnDefinition = "text") private String descripcion;
    @Column(length = 255) private String ruta;
    @Column(length = 100) private String icono;
    private Integer orden;
    @Column(nullable = false) private Boolean activo;
}
