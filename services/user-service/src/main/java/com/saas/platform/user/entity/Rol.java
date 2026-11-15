package com.saas.platform.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rol {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String nombre;

    /** null = rol del SISTEMA (global, solo lo edita la plataforma); con valor = rol propio de esa organización. */
    @Column(name = "organizacion_id")
    private Long organizacionId;
    
    @Column(length = 255)
    private String descripcion;
    
    @Column(name = "es_sistema", nullable = false)
    @Builder.Default
    private Boolean esSistema = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
    
    @OneToMany(mappedBy = "rol", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<RolModulo> permisos = new HashSet<>();
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Helper methods
    public void agregarPermiso(RolModulo permiso) {
        permisos.add(permiso);
        permiso.setRol(this);
    }
    
    public void removerPermiso(RolModulo permiso) {
        permisos.remove(permiso);
        permiso.setRol(null);
    }
}
