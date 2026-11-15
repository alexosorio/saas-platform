package com.saas.platform.user.repository;

import com.saas.platform.user.entity.RolModulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolModuloRepository extends JpaRepository<RolModulo, Long> {
    
    List<RolModulo> findByRolId(Long rolId);
    
    Optional<RolModulo> findByRolIdAndModuloId(Long rolId, Long moduloId);
    
    @Query("SELECT rm FROM RolModulo rm WHERE rm.rol.id = :rolId AND rm.leer = true")
    List<RolModulo> findModulosLegiblesByRolId(@Param("rolId") Long rolId);
    
    /** Borrado masivo inmediato: se ejecuta ANTES de re-insertar los permisos del rol. */
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RolModulo rm WHERE rm.rol.id = :rolId")
    void deleteByRolId(@Param("rolId") Long rolId);
}
