package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.NotaCreditoCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface NotaCreditoClienteRepository extends JpaRepository<NotaCreditoCliente, Long> {
    /** Notas con saldo a favor disponible, las más antiguas primero (se consumen en ese orden). */
    List<NotaCreditoCliente> findByClienteIdAndEstadoOrderByCreatedAtAsc(Long clienteId, String estado);
}
