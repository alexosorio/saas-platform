package com.saas.platform.catalog.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Entity @Table(name = "ciudades_organizaciones")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CiudadOrganizacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organizacion_id", nullable = false)
    private Long organizacionId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ciudad_id", nullable = false)
    private Ciudad ciudad;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
