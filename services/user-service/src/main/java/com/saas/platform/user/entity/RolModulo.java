package com.saas.platform.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "roles_modulos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolModulo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;
    
    @Column(name = "modulo_id", nullable = false)
    private Long moduloId;
    
    @Column(name = "modulo_codigo", length = 50)
    private String moduloCodigo;
    
    @Column(name = "modulo_nombre", length = 100)
    private String moduloNombre;

    /** Módulo real (solo lectura). Fuente de verdad de código y nombre aunque las columnas copiadas estén vacías. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "modulo_id", insertable = false, updatable = false)
    private Modulo modulo;

    public String codigoEfectivo() {
        if (modulo != null && modulo.getCodigo() != null) return modulo.getCodigo();
        return moduloCodigo;
    }

    public String nombreEfectivo() {
        if (modulo != null && modulo.getNombre() != null) return modulo.getNombre();
        return moduloNombre;
    }
    
    // Permisos CRUD
    @Column(nullable = false)
    @Builder.Default
    private Boolean crear = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean leer = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean actualizar = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean eliminar = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Helper method para verificar si tiene algún permiso
    public boolean tieneAlgunPermiso() {
        return crear || leer || actualizar || eliminar;
    }
    
    // Helper method para permisos completos
    public void darPermisosCompletos() {
        this.crear = true;
        this.leer = true;
        this.actualizar = true;
        this.eliminar = true;
    }
}
