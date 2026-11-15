package com.saas.platform.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "perfiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Perfil {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;
    
    @Column(length = 100)
    private String nombre;
    
    @Column(length = 100)
    private String apellido;
    
    @Column(length = 20)
    private String telefono;
    
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
    
    @Column(length = 10)
    private String idioma = "es";
    
    @Column(name = "zona_horaria", length = 50)
    private String zonaHoraria = "America/Bogota";
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
