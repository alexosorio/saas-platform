package com.saas.platform.catalog.service;

import com.saas.platform.catalog.dto.CargaMasivaResultDTO;
import com.saas.platform.catalog.entity.*;
import com.saas.platform.catalog.exception.BusinessException;
import com.saas.platform.catalog.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Carga masiva de productos desde CSV.
 *
 * Columnas (encabezado obligatorio, en este orden o por nombre):
 *   nombre, sku, codigo_barras (de la unidad), tipo_producto, categoria, marca, proveedor,
 *   precio_compra, precio_venta, descripcion, imei1, imei2, bodega,
 *   stock_inicial, activo
 *
 * Reglas:
 *  - El producto se identifica por SKU dentro de la organizacion.
 *    Si el SKU ya existe, se le AGREGAN unidades (no se duplica).
 *  - tipo_producto = 'movil'  -> requiere imei; cada fila = 1 unidad (stock 1).
 *  - tipo_producto = 'accesorio' -> sin imei; stock_inicial = N unidades.
 *  - bodega: se busca por codigo o nombre; si se indica, se crea inventario.
 *  - categoria/marca/proveedor: se buscan por nombre; categoria es obligatoria
 *    (si no existe, error en esa fila).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductoBulkService {

    private final ProductoRepository productoRepo;
    private final CategoriaRepository categoriaRepo;
    private final MarcaRepository marcaRepo;
    private final ProveedorRepository proveedorRepo;
    private final com.saas.platform.catalog.service.CuentaService cuentaService;
    private final UnidadRepository unidadRepo;
    private final BodegaRepository bodegaRepo;
    private final InventarioRepository inventarioRepo;
    private final ProductoService productoService;
    private final CaracteristicaProductoRepository caracteristicaRepo;
    private final ValorCaracteristicaRepository valorRepo;

    private static final String[] COLS = {
        "nombre","sku","codigo_barras","tipo_producto","categoria","marca","proveedor",
        "precio_compra","precio_venta","descripcion","imei1","imei2",
        "bodega","stock_inicial","activo","color"
    };
    // Además: cualquier columna "carac_<codigo>" asigna esa característica a cada unidad de la fila
    // (p. ej. carac_almacenamiento). "color" es un atajo de "carac_color".

    @Transactional
    public CargaMasivaResultDTO cargar(MultipartFile file, Long orgId, Long usuarioId) {
        if (file == null || file.isEmpty()) throw new BusinessException("Archivo vacio");

        int creados = 0, existentes = 0, unidades = 0, errores = 0;
        List<String> msgs = new ArrayList<>();

        // Caches para no repetir busquedas
        Map<String, Producto> productosPorSku = new HashMap<>();
        int autoCodigo = 1;   // contador para generar codigos unicos si no hay imei

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = br.readLine();
            if (headerLine == null) throw new BusinessException("CSV sin encabezado");
            Map<String, Integer> idx = mapearColumnas(headerLine);
            Map<String, CaracteristicaProducto> caracCols = columnasCaracteristicas(idx, msgs);

            String line; int fila = 1;
            while ((line = br.readLine()) != null) {
                fila++;
                if (line.isBlank()) continue;
                try {
                    String[] cols = parseCsvLine(line);

                    String nombre = get(cols, idx, "nombre");
                    String sku = get(cols, idx, "sku");
                    String tipo = orDefault(get(cols, idx, "tipo_producto"), "accesorio").toLowerCase();
                    boolean esMovil = "movil".equals(tipo) || "móvil".equals(tipo);

                    if (nombre == null || nombre.isBlank())
                        throw new BusinessException("nombre vacio");
                    if (sku == null || sku.isBlank())
                        throw new BusinessException("sku vacio (obligatorio para identificar el producto)");

                    // ── Resolver/crear el producto por SKU ──────────────────
                    Producto producto = productosPorSku.get(sku);
                    if (producto == null) {
                        Optional<Producto> existente = productoRepo.findBySkuAndOrganizacionId(sku, orgId);
                        if (existente.isPresent()) {
                            producto = existente.get();
                            existentes++;
                        } else {
                            Categoria categoria = buscarCategoria(get(cols, idx, "categoria"));
                            Marca marca = buscarMarca(get(cols, idx, "marca"));

                            producto = Producto.builder()
                                .organizacionId(orgId).nombre(nombre).sku(sku)
                                .descripcion(get(cols, idx, "descripcion"))
                                .categoria(categoria).marca(marca)
                                .tipoProducto(esMovil ? "movil" : "accesorio")
                                .requiereImei(esMovil)
                                .activo(parseBool(get(cols, idx, "activo"), true))
                                .build();
                            producto = productoRepo.save(producto);
                            creados++;
                        }
                        productosPorSku.put(sku, producto);
                    }

                    // ── Crear unidad(es) ────────────────────────────────────
                    // Los precios pertenecen a la UNIDAD: se toman de la fila
                    BigDecimal pCompraUnidad = parseMoney(get(cols, idx, "precio_compra"));
                    if (pCompraUnidad == null)
                        throw new BusinessException("precio_compra es obligatorio (es el costo de la unidad)");
                    BigDecimal pVentaUnidad = parseMoney(get(cols, idx, "precio_venta"));
                    Bodega bodega = buscarBodega(get(cols, idx, "bodega"));
                    Proveedor proveedor = buscarProveedor(get(cols, idx, "proveedor"), orgId);
                    // Código de barras del empaque: pertenece a la(s) unidad(es) de esta fila
                    String codigoBarras = UnidadService.normalizarCodigoBarras(get(cols, idx, "codigo_barras"));

                    if (esMovil) {
                        // 1 fila = 1 aparato con sus IMEIs
                        String imei1 = com.saas.platform.catalog.util.ImeiValidator.validar(get(cols, idx, "imei1"), "imei1");
                        String imei2 = com.saas.platform.catalog.util.ImeiValidator.validar(get(cols, idx, "imei2"), "imei2");
                        String codigo = (imei1 != null && !imei1.isBlank()) ? imei1
                            : sku + "-" + System.currentTimeMillis() + "-" + (autoCodigo++);
                        if (unidadRepo.existsByCodigoUnico(codigo))
                            throw new BusinessException("codigo/imei duplicado: " + codigo);
                        Unidad u = Unidad.builder()
                            .producto(producto).proveedor(proveedor).codigoUnico(codigo).codigoBarras(codigoBarras)
                            .imei1(emptyToNull(imei1)).imei2(emptyToNull(imei2))
                            .condicion(Unidad.Condicion.nuevo)
                            .precioCompra(pCompraUnidad).precioVenta(pVentaUnidad)
                            .estado(Unidad.EstadoUnidad.disponible).build();
                        u = unidadRepo.save(u);
                        guardarCaracteristicas(u, cols, idx, caracCols);
                        if (bodega != null) inventarioRepo.save(Inventario.builder().bodega(bodega).unidad(u).cantidad(1).build());
                        cuentaService.crearCxPParaUnidad(u);
                        unidades++;
                    } else {
                        // accesorio: stock_inicial unidades sin imei
                        int stock = (int) Math.max(0, parseLong(get(cols, idx, "stock_inicial"), 0));
                        for (int i = 0; i < stock; i++) {
                            String codigo = sku + "-" + System.currentTimeMillis() + "-" + (autoCodigo++);
                            Unidad u = Unidad.builder()
                                .producto(producto).proveedor(proveedor).codigoUnico(codigo).codigoBarras(codigoBarras)
                                .condicion(Unidad.Condicion.nuevo)
                                .precioCompra(pCompraUnidad).precioVenta(pVentaUnidad)
                                .estado(Unidad.EstadoUnidad.disponible).build();
                            u = unidadRepo.save(u);
                            guardarCaracteristicas(u, cols, idx, caracCols);
                            cuentaService.crearCxPParaUnidad(u);
                            if (bodega != null) inventarioRepo.save(Inventario.builder().bodega(bodega).unidad(u).cantidad(1).build());
                            unidades++;
                        }
                    }
                } catch (Exception ex) {
                    errores++;
                    msgs.add("Fila " + fila + ": " + ex.getMessage());
                    log.warn("Error fila {}: {}", fila, ex.getMessage());
                }
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException("Error leyendo el CSV: " + e.getMessage());
        }

        log.info("Carga masiva: creados={} existentes={} unidades={} errores={}", creados, existentes, unidades, errores);
        return CargaMasivaResultDTO.builder()
            .productosCreados(creados).productosExistentes(existentes)
            .unidadesCreadas(unidades).filasConError(errores).errores(msgs).build();
    }

    // ── Helpers de parsing ───────────────────────────────────────────────


    /** Columnas "color" y "carac_<codigo>" presentes en el encabezado, resueltas contra las características existentes. */
    private Map<String, CaracteristicaProducto> columnasCaracteristicas(Map<String, Integer> idx, List<String> msgs) {
        Map<String, CaracteristicaProducto> out = new LinkedHashMap<>();
        for (String col : idx.keySet()) {
            String codigo = col.equals("color") ? "color" : col.startsWith("carac_") ? col.substring(6) : null;
            if (codigo == null || codigo.isBlank()) continue;
            Optional<CaracteristicaProducto> c = caracteristicaRepo.findByCodigo(codigo);
            if (c.isPresent()) out.put(col, c.get());
            else msgs.add("Columna '" + col + "' ignorada: no existe la característica con código '" + codigo + "'");
        }
        return out;
    }

    private void guardarCaracteristicas(Unidad u, String[] cols, Map<String, Integer> idx,
                                        Map<String, CaracteristicaProducto> caracCols) {
        caracCols.forEach((col, carac) -> {
            String v = get(cols, idx, col);
            if (v == null) return;
            if ("color".equalsIgnoreCase(carac.getCodigo())) v = ResumenUnidadesService.normalizarColor(v);
            valorRepo.save(ValorCaracteristica.builder().unidad(u).caracteristica(carac).valor(v).build());
        });
    }

    private Map<String, Integer> mapearColumnas(String header) {
        String[] hs = parseCsvLine(header);
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < hs.length; i++) {
            String h = hs[i].trim().toLowerCase().replace(" ", "_").replace("ó", "o");
            idx.put(h, i);
        }
        // Validar que existan las columnas minimas
        for (String req : new String[]{"nombre", "sku"}) {
            if (!idx.containsKey(req))
                throw new BusinessException("Falta la columna obligatoria '" + req + "' en el encabezado");
        }
        return idx;
    }

    private String[] parseCsvLine(String line) {
        // Soporta separador coma o punto y coma; respeta comillas dobles
        char sep = line.contains(";") && !line.contains(",") ? ';' : ',';
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') { inQuotes = !inQuotes; }
            else if (c == sep && !inQuotes) { out.add(sb.toString()); sb.setLength(0); }
            else sb.append(c);
        }
        out.add(sb.toString());
        return out.toArray(new String[0]);
    }

    private String get(String[] cols, Map<String, Integer> idx, String col) {
        Integer i = idx.get(col);
        if (i == null || i >= cols.length) return null;
        String v = cols[i].trim();
        return v.isEmpty() ? null : v;
    }

    private Categoria buscarCategoria(String nombre) {
        if (nombre == null || nombre.isBlank())
            throw new BusinessException("categoria vacia (obligatoria)");
        return categoriaRepo.findAll().stream()
            .filter(c -> c.getNombre().equalsIgnoreCase(nombre.trim()))
            .findFirst().orElseThrow(() -> new BusinessException("categoria no existe: " + nombre));
    }

    private Marca buscarMarca(String nombre) {
        if (nombre == null || nombre.isBlank()) return null;
        return marcaRepo.findAll().stream()
            .filter(m -> m.getNombre().equalsIgnoreCase(nombre.trim()))
            .findFirst().orElse(null);
    }

    private Proveedor buscarProveedor(String nombre, Long orgId) {
        if (nombre == null || nombre.isBlank()) return null;
        return proveedorRepo.findByNombreIgnoreCaseAndOrganizacionId(nombre.trim(), orgId).orElse(null);
    }

    private Bodega buscarBodega(String nombreOCodigo) {
        if (nombreOCodigo == null || nombreOCodigo.isBlank()) return null;
        String q = nombreOCodigo.trim();
        return bodegaRepo.findAll().stream()
            .filter(b -> q.equalsIgnoreCase(b.getCodigo()) || q.equalsIgnoreCase(b.getNombre()))
            .findFirst().orElse(null);
    }

    private BigDecimal parseMoney(String s) {
        if (s == null || s.isBlank()) return null;
        try { return new BigDecimal(s.replace("$", "").replace(".", "").replace(",", ".").trim()); }
        catch (Exception e) {
            try { return new BigDecimal(s.replace("$", "").replace(",", "").trim()); }
            catch (Exception e2) { throw new BusinessException("precio invalido: " + s); }
        }
    }

    private long parseLong(String s, long def) {
        if (s == null || s.isBlank()) return def;
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return def; }
    }

    private boolean parseBool(String s, boolean def) {
        if (s == null || s.isBlank()) return def;
        String v = s.trim().toLowerCase();
        return v.equals("1") || v.equals("true") || v.equals("si") || v.equals("sí") || v.equals("x");
    }

    private String orDefault(String s, String def) { return (s == null || s.isBlank()) ? def : s; }
    private String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s; }
    private BigDecimal orElse(BigDecimal a, BigDecimal b, BigDecimal c) { return a != null ? a : (b != null ? b : c); }
}
