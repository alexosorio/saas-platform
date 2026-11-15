package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.DetalleDevolucion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface DetalleDevolucionRepository extends JpaRepository<DetalleDevolucion, Long> {
    /** Cantidad ya devuelta de una línea de la orden. */
    @Query("SELECT COALESCE(SUM(d.cantidad), 0) FROM DetalleDevolucion d WHERE d.detalleOrdenId = :detalleId")
    Integer cantidadDevuelta(@Param("detalleId") Long detalleId);
}
