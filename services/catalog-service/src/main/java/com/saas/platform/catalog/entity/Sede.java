package com.saas.platform.catalog.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
@Entity @Table(name = "sedes")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Sede {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ciudad_organizacion_id", nullable = false)
    private CiudadOrganizacion ciudadOrganizacion;
    @Column(nullable = false, length = 200) private String nombre;
    @Column(columnDefinition = "text") private String direccion;
    @Column(length = 20) private String telefono;
    @Column(length = 255) private String email;
    @Column(nullable = false) @Builder.Default private Boolean activo = true;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
