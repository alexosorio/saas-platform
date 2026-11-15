package com.saas.platform.catalog.service;

import com.saas.platform.catalog.dto.*;
import com.saas.platform.catalog.entity.EnlacePublicoLista;
import com.saas.platform.catalog.entity.Cliente;
import com.saas.platform.notifications.NotificationMessage;
import com.saas.platform.notifications.NotificationPublisher;
import com.saas.platform.catalog.entity.ListaPrecios;
import com.saas.platform.catalog.entity.Precio;
import com.saas.platform.catalog.exception.BusinessException;
import com.saas.platform.catalog.exception.InvalidOperationException;
import com.saas.platform.catalog.exception.ResourceNotFoundException;
import com.saas.platform.catalog.repository.EnlacePublicoListaRepository;
import com.saas.platform.catalog.repository.InventarioRepository;
import com.saas.platform.catalog.repository.ListaPreciosRepository;
import com.saas.platform.catalog.repository.PrecioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class EnlacePublicoService {

    private final EnlacePublicoListaRepository repo;
    private final ListaPreciosRepository listaRepo;
    private final PrecioRepository precioRepo;
    private final PrecioCalculadoService precioCalculadoService;
    private final InventarioRepository inventarioRepo;
    private final EmailService emailService;
    private final WhatsAppService whatsAppService;
    private final com.saas.platform.catalog.repository.ClienteRepository clienteRepo;
    private final NotificationPublisher notificationPublisher;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;

    /** Nombre de la organización y si tiene logo (marca en correos y en la página pública). */
    private Object[] marcaOrg(Long orgId) {
        java.util.List<?> r = em.createNativeQuery("SELECT nombre, (logo IS NOT NULL) FROM organizaciones WHERE id = :o")
            .setParameter("o", orgId).getResultList();
        return r.isEmpty() ? new Object[]{ null, false } : (Object[]) r.get(0);
    }

    @Value("${app.public-base-url:http://localhost:5174}")
    private String publicBaseUrl;

    @Transactional
    public EnlacePublicoDTO crear(Long listaId, CreateEnlacePublicoRequest req, Long orgId, Long usuarioId) {
        ListaPrecios lista = listaRepo.findById(listaId)
            .orElseThrow(() -> new ResourceNotFoundException("Lista no encontrada"));
        if (!lista.getOrganizacionId().equals(orgId))
            throw new ResourceNotFoundException("Lista no encontrada");

        String token = UUID.randomUUID().toString().replace("-", "");

        EnlacePublicoLista e = EnlacePublicoLista.builder()
            .listaPrecios(lista).token(token)
            .nombreEnlace(req.getNombreEnlace()).descripcion(req.getDescripcion())
            .activo(true)
            .fechaExpiracion(req.getFechaExpiracion()).maxVistas(req.getMaxVistas())
            .mostrarPrecios(req.getMostrarPrecios() != null ? req.getMostrarPrecios() : true)
            .mostrarStock(req.getMostrarStock() != null ? req.getMostrarStock() : false)
            .permitirBusqueda(req.getPermitirBusqueda() != null ? req.getPermitirBusqueda() : true)
            .usuarioCreador(usuarioId).vistasActuales(0).build();

        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            e.setRequierePassword(true);
            e.setPasswordHash(bcrypt.encode(req.getPassword()));
        }
        log.info("Enlace publico creado lista={} token={}", listaId, token);
        return toDTO(repo.save(e));
    }

    @Transactional(readOnly = true)
    public List<EnlacePublicoDTO> listarPorLista(Long listaId, Long orgId) {
        ListaPrecios lista = listaRepo.findById(listaId)
            .orElseThrow(() -> new ResourceNotFoundException("Lista no encontrada"));
        if (!lista.getOrganizacionId().equals(orgId))
            throw new ResourceNotFoundException("Lista no encontrada");
        return repo.findByListaPreciosIdOrderByCreatedAtDesc(listaId)
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public void eliminar(Long enlaceId, Long orgId) {
        EnlacePublicoLista e = repo.findById(enlaceId)
            .orElseThrow(() -> new ResourceNotFoundException("Enlace no encontrado"));
        if (!e.getListaPrecios().getOrganizacionId().equals(orgId))
            throw new ResourceNotFoundException("Enlace no encontrado");
        repo.delete(e);
    }

    @Transactional
    public EnlacePublicoDTO desactivar(Long enlaceId, Long orgId) {
        EnlacePublicoLista e = repo.findById(enlaceId)
            .orElseThrow(() -> new ResourceNotFoundException("Enlace no encontrado"));
        if (!e.getListaPrecios().getOrganizacionId().equals(orgId))
            throw new ResourceNotFoundException("Enlace no encontrado");
        e.setActivo(false);
        return toDTO(repo.save(e));
    }

    /**
     * Consulta PUBLICA por token. Valida activo, expiracion, max vistas, password.
     * Incrementa contador de vistas. NO requiere autenticacion.
     */
    @Transactional
    public VistaPublicaDTO consultarPorToken(String token, String password) {
        EnlacePublicoLista e = repo.findByToken(token)
            .orElseThrow(() -> new ResourceNotFoundException("Enlace no encontrado"));

        if (!Boolean.TRUE.equals(e.getActivo()))
            throw new InvalidOperationException("Este enlace fue desactivado");

        if (e.getFechaExpiracion() != null && e.getFechaExpiracion().isBefore(LocalDateTime.now()))
            throw new InvalidOperationException("Este enlace ha expirado");

        if (e.getMaxVistas() != null && e.getVistasActuales() != null
                && e.getVistasActuales() >= e.getMaxVistas())
            throw new InvalidOperationException("Este enlace alcanzo el limite de vistas");

        if (Boolean.TRUE.equals(e.getRequierePassword())) {
            if (password == null || password.isBlank())
                throw new BusinessException("Este enlace requiere contraseña");
            if (!bcrypt.matches(password, e.getPasswordHash()))
                throw new BusinessException("Contraseña incorrecta");
        }

        // Incrementar contador de vistas
        e.setVistasActuales((e.getVistasActuales() != null ? e.getVistasActuales() : 0) + 1);
        repo.save(e);

        // Armar la vista publica: TODOS los productos activos con su precio
        // calculado segun la formula de la lista (modelo nuevo).
        ListaPrecios lista = e.getListaPrecios();
        List<PrecioCalculadoDTO> calculados =
            precioCalculadoService.calcularParaLista(lista.getId(), lista.getOrganizacionId());

        List<ItemPublicoDTO> items = calculados.stream().map(pc ->
            ItemPublicoDTO.builder()
                .productoId(pc.getProductoId())
                .nombre(pc.getNombre())
                .sku(pc.getSku())
                .descripcion(null)
                .imagenUrl(pc.getImagenUrl())
                .categoriaNombre(pc.getCategoriaNombre())
                .marcaNombre(pc.getMarcaNombre())
                .precio(e.getMostrarPrecios() ? pc.getPrecioMinimo() : null)
                .precioHasta(e.getMostrarPrecios() ? pc.getPrecioMaximo() : null)
                .colores(pc.getColores())
                .stockTotal(e.getMostrarStock() ? pc.getStockTotal() : null)
                .build()
        ).collect(Collectors.toList());

        Object[] marca = marcaOrg(lista.getOrganizacionId());
        return VistaPublicaDTO.builder()
            .organizacionId(lista.getOrganizacionId())
            .organizacionNombre((String) marca[0])
            .tieneLogo(Boolean.TRUE.equals(marca[1]))
            .nombreLista(lista.getNombre())
            .tipoCliente(lista.getTipoCliente() != null ? lista.getTipoCliente().name() : null)
            .descripcion(lista.getDescripcion())
            .nombreEnlace(e.getNombreEnlace())
            .mostrarPrecios(e.getMostrarPrecios())
            .mostrarStock(e.getMostrarStock())
            .permitirBusqueda(e.getPermitirBusqueda())
            .items(items).build();
    }

    // ── Envío del enlace por correo / WhatsApp (desde el backend) ─────────

    @Transactional(readOnly = true)
    public void enviarPorEmail(Long enlaceId, Long orgId, String destinatario, String mensajeExtra) {
        EnlacePublicoLista e = resolverEnlace(enlaceId, orgId);
        String url = publicBaseUrl + "/public/list/" + e.getToken();
        ListaPrecios lista = e.getListaPrecios();
        String asunto = "Lista de precios: " + lista.getNombre();
        String html = construirHtmlCorreo(lista, e, url, mensajeExtra);
        Long orgLista = lista.getOrganizacionId();
        notificationPublisher.publicar(NotificationMessage.email(destinatario, asunto, html, orgLista, (String) marcaOrg(orgLista)[0]));
        log.info("Lista {} encolada (correo) para {}", lista.getId(), destinatario);
    }

    @Transactional(readOnly = true)
    public void enviarPorWhatsApp(Long enlaceId, Long orgId, String telefono, String mensajeExtra) {
        EnlacePublicoLista e = resolverEnlace(enlaceId, orgId);
        String url = publicBaseUrl + "/public/list/" + e.getToken();
        ListaPrecios lista = e.getListaPrecios();
        StringBuilder sb = new StringBuilder();
        sb.append("Hola \uD83D\uDC4B, te comparto ").append(lista.getNombre());
        if (mensajeExtra != null && !mensajeExtra.isBlank()) sb.append("\n\n").append(mensajeExtra);
        sb.append("\n\n").append(url);
        notificationPublisher.publicar(NotificationMessage.whatsapp(telefono, sb.toString()));
        log.info("Lista {} encolada (WhatsApp) para {}", lista.getId(), telefono);
    }

    /** Envía el enlace a varios clientes seleccionados, por correo y/o WhatsApp. */
    @Transactional(readOnly = true)
    public EnvioMasivoResultDTO enviarMasivo(Long enlaceId, Long orgId, EnvioMasivoRequest req) {
        EnlacePublicoLista e = resolverEnlace(enlaceId, orgId);
        String url = publicBaseUrl + "/public/list/" + e.getToken();
        ListaPrecios lista = e.getListaPrecios();
        String canal = req.getCanal() == null ? "ambos" : req.getCanal().toLowerCase();
        boolean porEmail = canal.equals("email") || canal.equals("ambos");
        boolean porWhats = canal.equals("whatsapp") || canal.equals("ambos");

        if (req.getClienteIds() == null || req.getClienteIds().isEmpty())
            throw new BusinessException("Debes seleccionar al menos un cliente");

        List<Cliente> clientes = clienteRepo.findByOrganizacionIdAndIdIn(orgId, req.getClienteIds());
        EnvioMasivoResultDTO result = new EnvioMasivoResultDTO();

        String asunto = "Lista de precios: " + lista.getNombre();
        String html = porEmail ? construirHtmlCorreo(lista, e, url, req.getMensaje()) : null;
        String orgNombre = (String) marcaOrg(orgId)[0];

        StringBuilder waMsg = new StringBuilder();
        waMsg.append("Hola \uD83D\uDC4B, te comparto ").append(lista.getNombre());
        if (req.getMensaje() != null && !req.getMensaje().isBlank()) waMsg.append("\n\n").append(req.getMensaje());
        waMsg.append("\n\n").append(url);
        String textoWa = waMsg.toString();

        for (Cliente c : clientes) {
            if (porEmail) {
                if (c.getEmail() == null || c.getEmail().isBlank()) {
                    result.add(item(c, "email", null, "OMITIDO", "Cliente sin correo"));
                } else {
                    try { notificationPublisher.publicar(NotificationMessage.email(c.getEmail(), asunto, html, orgId, orgNombre));
                        result.add(item(c, "email", c.getEmail(), "ENCOLADO", null));
                    } catch (Exception ex) {
                        result.add(item(c, "email", c.getEmail(), "FALLIDO", ex.getMessage()));
                    }
                }
            }
            if (porWhats) {
                if (c.getTelefono() == null || c.getTelefono().isBlank()) {
                    result.add(item(c, "whatsapp", null, "OMITIDO", "Cliente sin teléfono"));
                } else {
                    try { notificationPublisher.publicar(NotificationMessage.whatsapp(c.getTelefono(), textoWa));
                        result.add(item(c, "whatsapp", c.getTelefono(), "ENCOLADO", null));
                    } catch (Exception ex) {
                        result.add(item(c, "whatsapp", c.getTelefono(), "FALLIDO", ex.getMessage()));
                    }
                }
            }
        }
        log.info("Envío masivo lista={} canal={} -> {} enviados, {} omitidos, {} fallidos",
            lista.getId(), canal, result.getEnviados(), result.getOmitidos(), result.getFallidos());
        return result;
    }

    private EnvioMasivoResultDTO.Item item(Cliente c, String canal, String destino, String estado, String motivo) {
        return EnvioMasivoResultDTO.Item.builder()
            .clienteId(c.getId()).nombre(c.getNombre()).canal(canal)
            .destino(destino).estado(estado).motivo(motivo).build();
    }

    private EnlacePublicoLista resolverEnlace(Long enlaceId, Long orgId) {
        EnlacePublicoLista e = repo.findById(enlaceId)
            .orElseThrow(() -> new ResourceNotFoundException("Enlace no encontrado"));
        if (e.getListaPrecios() == null || !e.getListaPrecios().getOrganizacionId().equals(orgId))
            throw new ResourceNotFoundException("Enlace no encontrado");
        if (Boolean.FALSE.equals(e.getActivo()))
            throw new BusinessException("El enlace está desactivado");
        return e;
    }

    /** Arma un correo HTML con intro, botón al catálogo y (opcional) una tabla de precios. */
    private String construirHtmlCorreo(ListaPrecios lista, EnlacePublicoLista e, String url, String mensajeExtra) {
        StringBuilder filas = new StringBuilder();
        if (Boolean.TRUE.equals(e.getMostrarPrecios())) {
            List<PrecioCalculadoDTO> precios =
                precioCalculadoService.calcularParaLista(lista.getId(), lista.getOrganizacionId());
            int limite = 50, i = 0;
            for (PrecioCalculadoDTO p : precios) {
                if (i++ >= limite) break;
                // Precio por unidad: si las unidades tienen precios distintos se muestra "Desde"
                String precioTxt = p.getPrecioMinimo() == null ? "A consultar"
                    : (p.getPrecioMaximo() != null && p.getPrecioMaximo().compareTo(p.getPrecioMinimo()) > 0 ? "Desde " : "")
                      + "$" + String.format("%,.0f", p.getPrecioMinimo());
                String coloresTxt = p.getColores() == null || p.getColores().isEmpty() ? "" : String.join(", ", p.getColores());
                filas.append("<tr>")
                     .append("<td style='padding:8px;border-bottom:1px solid #eee'>").append(esc(p.getNombre())).append("</td>")
                     .append("<td style='padding:8px;border-bottom:1px solid #eee;color:#666'>").append(esc(coloresTxt)).append("</td>")
                     .append("<td style='padding:8px;border-bottom:1px solid #eee;text-align:right;font-weight:bold;color:#693a84'>").append(precioTxt).append("</td>")
                     .append("</tr>");
            }
        }
        String tabla = filas.length() == 0 ? "" :
            "<table style='width:100%;border-collapse:collapse;margin:16px 0;font-size:14px'>"
            + "<tr style='background:#f4f1f6'><th style='padding:8px;text-align:left'>Producto</th>"
            + "<th style='padding:8px;text-align:left'>Colores</th>"
            + "<th style='padding:8px;text-align:right'>Precio</th></tr>"
            + filas + "</table>";

        String intro = (mensajeExtra != null && !mensajeExtra.isBlank())
            ? "<p style='color:#444'>" + esc(mensajeExtra) + "</p>" : "";

        // Solo contenido interno: el marco (encabezado de marca + firma) lo pone la plantilla base.
        return (lista.getDescripcion() != null ? "<p style='color:#666;margin-top:0'>" + esc(lista.getDescripcion()) + "</p>" : "")
            + intro
            + "<p><a href='" + url + "' style='display:inline-block;background:#a871bd;color:#fff;"
            + "padding:12px 24px;border-radius:6px;text-decoration:none;font-weight:bold'>Ver catálogo completo</a></p>"
            + tabla
            + "<p style='color:#999;font-size:12px;margin-top:24px'>Si el botón no funciona, copia este enlace: " + url + "</p>";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private EnlacePublicoDTO toDTO(EnlacePublicoLista e) {
        return EnlacePublicoDTO.builder()
            .id(e.getId())
            .listaPreciosId(e.getListaPrecios() != null ? e.getListaPrecios().getId() : null)
            .token(e.getToken()).nombreEnlace(e.getNombreEnlace()).descripcion(e.getDescripcion())
            .requierePassword(e.getRequierePassword()).activo(e.getActivo())
            .fechaCreacion(e.getFechaCreacion()).fechaExpiracion(e.getFechaExpiracion())
            .maxVistas(e.getMaxVistas()).vistasActuales(e.getVistasActuales())
            .mostrarPrecios(e.getMostrarPrecios()).mostrarStock(e.getMostrarStock())
            .permitirBusqueda(e.getPermitirBusqueda())
            .urlPublica(publicBaseUrl + "/public/list/" + e.getToken())
            .build();
    }
}
