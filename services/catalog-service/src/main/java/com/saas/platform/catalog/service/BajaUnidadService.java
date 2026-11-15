package com.saas.platform.catalog.service;

import com.saas.platform.catalog.dto.UnidadDTO;
import com.saas.platform.catalog.entity.*;
import com.saas.platform.catalog.exception.InvalidOperationException;
import com.saas.platform.catalog.exception.ResourceNotFoundException;
import com.saas.platform.catalog.repository.*;
import com.saas.platform.catalog.security.UserPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Las unidades NO se eliminan: se DAN DE BAJA con motivo. En una sola transacción:
 *  1. Movimiento de SALIDA en inventario por las existencias de la unidad.
 *  2. Según el tipo:
 *       error_registro / devolucion_proveedor -> NOTA CRÉDITO sobre la cuenta por pagar (ya no se le debe)
 *       dano_perdida                          -> la cuenta por pagar se mantiene (se le sigue debiendo)
 *  3. Unidad en estado 'dado_de_baja' con tipo, motivo, fecha y usuario.
 *  4. Registro de auditoría.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BajaUnidadService {

    public static final Map<String, String> TIPOS = Map.of(
        "error_registro", "Error de registro",
        "devolucion_proveedor", "Devolución al proveedor",
        "dano_perdida", "Daño o pérdida");

    private final UnidadRepository unidadRepo;
    private final InventarioRepository inventarioRepo;
    private final MovimientoInventarioRepository movimientoRepo;
    private final CuentaPorPagarRepository cxpRepo;
    private final CuentaService cuentaService;
    private final AuditoriaService auditoria;
    private final UnidadService unidadService;
    @PersistenceContext private EntityManager em;

    @Transactional
    public UnidadDTO darDeBaja(Long unidadId, String tipo, String motivo, Long orgId) {
        if (!TIPOS.containsKey(tipo))
            throw new InvalidOperationException("Tipo de baja inválido. Usa: " + String.join(", ", TIPOS.keySet()));
        if (motivo == null || motivo.trim().length() < 10)
            throw new InvalidOperationException("Describe el motivo de la baja (mínimo 10 caracteres)");
        String obs = motivo.trim();

        Unidad u = unidadRepo.findById(unidadId).orElseThrow(() -> new ResourceNotFoundException("Unidad no encontrada"));
        if (u.getProducto() == null || !Objects.equals(u.getProducto().getOrganizacionId(), orgId))
            throw new ResourceNotFoundException("Unidad no encontrada");
        switch (u.getEstado()) {
            case vendido -> throw new InvalidOperationException("La unidad ya fue vendida: no se puede dar de baja. Si hubo devolución del cliente, gestiónala sobre la orden.");
            case reservado -> throw new InvalidOperationException("La unidad está reservada en una orden en proceso. Reversa la orden antes de darla de baja.");
            case dado_de_baja -> throw new InvalidOperationException("La unidad ya está dada de baja");
            default -> { }
        }
        // ¿Está incluida en una orden aún abierta?
        @SuppressWarnings("unchecked")
        List<Object> abiertas = em.createNativeQuery(
                "SELECT o.numero_orden FROM detalles_orden d JOIN ordenes o ON o.id = d.orden_id " +
                "WHERE d.unidad_id = :u AND CAST(o.estado AS varchar) IN ('borrador', 'en_proceso')")
            .setParameter("u", unidadId).getResultList();
        if (!abiertas.isEmpty())
            throw new InvalidOperationException("La unidad está en la orden " + abiertas.get(0) + ". Quítala de la orden antes de darla de baja.");

        Long usuarioId = actorId();
        String etiqueta = TIPOS.get(tipo);
        String motivoMovimiento = "Baja de unidad (" + etiqueta + "): " + obs;

        // 1) Salida de inventario
        int unidadesSalida = 0;
        for (Inventario inv : inventarioRepo.findByUnidadId(unidadId)) {
            int anterior = inv.getCantidad() != null ? inv.getCantidad() : 0;
            if (anterior <= 0) continue;
            inv.setCantidad(0);
            inv.setCantidadReservada(0);
            inventarioRepo.save(inv);
            movimientoRepo.save(MovimientoInventario.builder()
                .inventario(inv).tipo(MovimientoInventario.TipoMovimiento.salida)
                .cantidad(anterior).cantidadAnterior(anterior).cantidadNueva(0)
                .motivo(motivoMovimiento).usuarioId(usuarioId).build());
            unidadesSalida += anterior;
        }

        // 2) Efecto sobre la cuenta por pagar
        String efectoCxp = "Sin cuenta por pagar asociada";
        Optional<CuentaPorPagar> cxp = cxpRepo.findFirstByUnidadId(unidadId);
        if (cxp.isPresent()) {
            CuentaPorPagar c = cxp.get();
            if (tipo.equals("dano_perdida")) {
                efectoCxp = "Se mantiene la deuda con el proveedor (saldo " + c.getSaldo() + ")";
            } else {
                BigDecimal monto = u.getPrecioCompra() != null ? u.getPrecioCompra() : c.getMonto();
                cuentaService.aplicarNotaCredito(c, monto, "Nota crédito por baja de la unidad "
                    + u.getCodigoUnico() + " (" + etiqueta + "): " + obs, unidadId, usuarioId);
                efectoCxp = "Nota crédito por " + monto + "; la cuenta queda en estado " + c.getEstado();
            }
        }

        // 3) Unidad dada de baja
        u.setEstado(Unidad.EstadoUnidad.dado_de_baja);
        u.setTipoBaja(tipo);
        u.setMotivoBaja(obs);
        u.setFechaBaja(LocalDateTime.now());
        u.setUsuarioBaja(usuarioId);
        unidadRepo.save(u);

        // 4) Auditoría
        auditoria.registrar("UNIDAD_DADA_DE_BAJA", "unidad", unidadId, Map.of(
            "codigo", String.valueOf(u.getCodigoUnico()), "tipo", etiqueta, "motivo", obs,
            "salidaInventario", unidadesSalida, "cuentaPorPagar", efectoCxp));
        log.info("Unidad {} dada de baja ({}) por usuario {}: {}", u.getCodigoUnico(), tipo, usuarioId, efectoCxp);
        return unidadService.getById(unidadId);
    }

    private Long actorId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (a != null && a.getPrincipal() instanceof UserPrincipal p) ? p.getId() : null;
    }
}
