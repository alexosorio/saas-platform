package com.saas.platform.user.repository;

import com.saas.platform.user.entity.Parametro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ParametroRepository extends JpaRepository<Parametro, Long> {
    List<Parametro> findByOrganizacionIdIsNullOrderByCategoriaAscNombreAsc();
    Optional<Parametro> findByClaveAndOrganizacionIdIsNull(String clave);
    Optional<Parametro> findByClaveAndOrganizacionId(String clave, Long organizacionId);
    List<Parametro> findByOrganizacionId(Long organizacionId);
}
