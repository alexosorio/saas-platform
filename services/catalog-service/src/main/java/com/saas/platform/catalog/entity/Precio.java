package com.saas.platform.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "precios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Precio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lista_precio_id", nullable = false)
    private ListaPrecios listaPrecios;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "margen_porcentaje", precision = 5, scale = 2)
    private BigDecimal margenPorcentaje;

    @CreationTimestamp
    @Column(name = "vigente_desde", updatable = false)
    private LocalDateTime vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDateTime vigenteHasta;
}
