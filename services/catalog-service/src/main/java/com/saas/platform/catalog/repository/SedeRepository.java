package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.Sede;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface SedeRepository extends JpaRepository<Sede, Long> {
    List<Sede> findByCiudadOrganizacion_OrganizacionId(Long orgId);
    List<Sede> findByCiudadOrganizacion_OrganizacionIdAndActivoTrue(Long orgId);
}
