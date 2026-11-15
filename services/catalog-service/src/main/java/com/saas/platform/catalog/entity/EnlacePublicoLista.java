package com.saas.platform.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "enlaces_publicos_listas")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EnlacePublicoLista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lista_precio_id", nullable = false)
    private ListaPrecios listaPrecios;

    @Column(nullable = false, length = 100, unique = true)
    private String token;

    @Column(name = "nombre_enlace", length = 200)
    private String nombreEnlace;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(name = "requiere_password")
    @Builder.Default
    private Boolean requierePassword = false;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_expiracion")
    private LocalDateTime fechaExpiracion;

    @Column(name = "max_vistas")
    private Integer maxVistas;

    @Column(name = "vistas_actuales")
    @Builder.Default
    private Integer vistasActuales = 0;

    @Column(name = "mostrar_precios")
    @Builder.Default
    private Boolean mostrarPrecios = true;

    @Column(name = "mostrar_stock")
    @Builder.Default
    private Boolean mostrarStock = false;

    @Column(name = "permitir_busqueda")
    @Builder.Default
    private Boolean permitirBusqueda = true;

    @Column(name = "usuario_creador", nullable = false)
    private Long usuarioCreador;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // categorias_visibles y marcas_visibles son jsonb; los omitimos por simplicidad
    // (se mostraran todas las categorias/marcas que tengan precios en la lista)
}
