package com.saas.platform.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "valores_caracteristicas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"unidad", "caracteristica"})
@ToString(exclude = {"unidad", "caracteristica"})
public class ValorCaracteristica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidad_id", nullable = false)
    private Unidad unidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caracteristica_id", nullable = false)
    private CaracteristicaProducto caracteristica;

    @Column(columnDefinition = "text")
    private String valor;
}
