package com.saas.platform.user.repository;

import com.saas.platform.user.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    /**
     * Buscar usuario por email
     */
    Optional<Usuario> findByEmail(String email);
    
    /**
     * Verificar si existe un email
     */
    boolean existsByEmail(String email);

    List<Usuario> findByOrganizacionId(Long organizacionId);
    
    /**
     * Buscar usuario por token de recuperación
     */
    Optional<Usuario> findByTokenRecuperacion(String token);

    Optional<Usuario> findByTokenVerificacion(String token);
    
    /**
     * Buscar usuario con perfil (fetch join para evitar N+1)
     */
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.perfil WHERE u.id = :id")
    Optional<Usuario> findByIdWithPerfil(@Param("id") Long id);
    
    /**
     * Buscar usuario por email con perfil
     */
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.perfil WHERE u.email = :email")
    Optional<Usuario> findByEmailWithPerfil(@Param("email") String email);
}
