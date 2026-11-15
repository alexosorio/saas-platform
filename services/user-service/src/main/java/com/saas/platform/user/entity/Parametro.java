package com.saas.platform.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Parámetro configurable. organizacionId null = valor general; con valor = propio de esa organización. */
@Entity @Table(name = "parametros")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Parametro {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80) private String clave;
    @Column(name = "organizacion_id") private Long organizacionId;
    @Column(nullable = false, length = 500) private String valor;
    @Column(nullable = false, length = 20) private String tipo;
    @Column(nullable = false, length = 20) private String alcance;      // plataforma | organizacion
    @Column(length = 50) private String categoria;
    @Column(length = 120) private String nombre;
    @Column(columnDefinition = "text") private String descripcion;
    private BigDecimal minimo;
    private BigDecimal maximo;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @Column(name = "updated_by") private Long updatedBy;
}
