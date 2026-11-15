package com.saas.platform.user.repository;

import com.saas.platform.user.entity.Modulo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ModuloRepository extends JpaRepository<Modulo, Long> {
    List<Modulo> findByActivoTrueOrderByOrdenAscNombreAsc();
}
