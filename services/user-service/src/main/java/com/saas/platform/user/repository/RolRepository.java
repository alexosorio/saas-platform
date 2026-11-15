package com.saas.platform.user.repository;

import com.saas.platform.user.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {
    
    Optional<Rol> findByNombre(String nombre);
    
    boolean existsByNombre(String nombre);
    
    List<Rol> findByActivoTrue();
    
    @Query("SELECT r FROM Rol r LEFT JOIN FETCH r.permisos WHERE r.id = :id")
    Optional<Rol> findByIdWithPermisos(@Param("id") Long id);
    
    @Query("SELECT r FROM Rol r WHERE r.esSistema = false AND r.activo = true")
    List<Rol> findRolesEditables();
}
