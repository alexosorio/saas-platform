package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.ListaPrecios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface ListaPreciosRepository extends JpaRepository<ListaPrecios, Long> {
    List<ListaPrecios> findByOrganizacionId(Long organizacionId);
    List<ListaPrecios> findByOrganizacionIdAndActivaTrue(Long organizacionId);
    List<ListaPrecios> findByOrganizacionIdAndTipoCliente(Long organizacionId, ListaPrecios.TipoCliente tipo);
}
