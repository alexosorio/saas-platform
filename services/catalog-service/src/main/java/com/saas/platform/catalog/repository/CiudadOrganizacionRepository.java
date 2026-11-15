package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.CiudadOrganizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface CiudadOrganizacionRepository extends JpaRepository<CiudadOrganizacion, Long> {
    Optional<CiudadOrganizacion> findByOrganizacionIdAndCiudadId(Long organizacionId, Long ciudadId);
}
