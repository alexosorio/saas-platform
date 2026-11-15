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
@Table(name = "usuarios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @Column(nullable = false)
    private Boolean activo = true;
    
    @Column(name = "email_verificado")
    private Boolean emailVerificado = false;
    
    @Column(name = "codigo_verificacion")
    private String codigoVerificacion;
    
    @Column(name = "codigo_verificacion_expira")
    private LocalDateTime codigoVerificacionExpira;
    
    // Enlace de verificación de correo (distinto del código de 6 dígitos)
    @Column(name = "token_verificacion", length = 100)
    private String tokenVerificacion;

    @Column(name = "token_verificacion_expira")
    private LocalDateTime tokenVerificacionExpira;

    @Column(name = "verificacion_enviada_en")
    private LocalDateTime verificacionEnviadaEn;

    @Column(name = "token_recuperacion")
    private String tokenRecuperacion;
    
    @Column(name = "token_recuperacion_expira")
    private LocalDateTime tokenRecuperacionExpira;
    
    @Column(name = "intentos_fallidos")
    private Integer intentosFallidos = 0;
    
    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;
    
    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;
    
    @Column(name = "ultimo_cambio_password")
    private LocalDateTime ultimoCambioPassword;
    
    @Column(name = "organizacion_id")
    private Long organizacionId;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id")
    private Rol rol;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relación con perfil
    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Perfil perfil;
    
    // Métodos de utilidad
    public boolean estaBloqueado() {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(LocalDateTime.now());
    }
    
    public boolean codigoVerificacionValido(String codigo) {
        return codigoVerificacion != null 
            && codigoVerificacion.equals(codigo)
            && codigoVerificacionExpira != null
            && codigoVerificacionExpira.isAfter(LocalDateTime.now());
    }
    
    public boolean tokenRecuperacionValido(String token) {
        return tokenRecuperacion != null 
            && tokenRecuperacion.equals(token)
            && tokenRecuperacionExpira != null
            && tokenRecuperacionExpira.isAfter(LocalDateTime.now());
    }
    
    public void incrementarIntentosFallidos() {
        this.intentosFallidos = (this.intentosFallidos == null ? 0 : this.intentosFallidos) + 1;
        
        // Bloquear después de 5 intentos fallidos (15 minutos)
        if (this.intentosFallidos >= 5) {
            this.bloqueadoHasta = LocalDateTime.now().plusMinutes(15);
        }
    }
    
    public void resetearIntentosFallidos() {
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
    }
}
