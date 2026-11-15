package com.saas.platform.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "proveedores")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Proveedor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organizacion_id", nullable = false)
    private Long organizacionId;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 50)
    private String identificacion;   // NIT / RUT

    @Column(length = 50)
    private String telefono;

    @Column(length = 150)
    private String email;

    @Column(length = 300)
    private String direccion;

    @Column(length = 150)
    private String contacto;

    @Column(columnDefinition = "text")
    private String notas;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
