package com.saas.platform.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "productos")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Producto {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organizacion_id", nullable = false)
    private Long organizacionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marca_id")
    private Marca marca;

    @Column(nullable = false, length = 255)
    private String nombre;

    @Column(length = 100)
    private String sku;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    // Precios intrinsecos del producto


    // Clasificacion. 'movil' => requiere IMEI. 'accesorio' => no.
    @Column(name = "tipo_producto", length = 50)
    @Builder.Default
    private String tipoProducto = "accesorio";

    @Column(name = "requiere_imei", nullable = false)
    @Builder.Default
    private Boolean requiereImei = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
