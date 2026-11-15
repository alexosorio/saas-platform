package com.saas.platform.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventario", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"bodega_id", "unidad_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bodega_id", nullable = false)
    private Bodega bodega;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidad_id", nullable = false)
    private Unidad unidad;

    @Column(nullable = false)
    @Builder.Default
    private Integer cantidad = 1;

    @Column(name = "cantidad_reservada", nullable = false)
    @Builder.Default
    private Integer cantidadReservada = 0;

    /** Stock disponible real = existencias - reservado (bloqueado por pagos en proceso). */
    @Transient
    public int getDisponible() {
        int c = cantidad != null ? cantidad : 0;
        int r = cantidadReservada != null ? cantidadReservada : 0;
        return c - r;
    }

    @Column(length = 100)
    private String ubicacion;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
