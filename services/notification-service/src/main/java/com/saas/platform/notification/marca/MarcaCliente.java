package com.saas.platform.notification.marca;

import com.saas.platform.email.Marca;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Obtiene el logo de una organización desde user-service (endpoint público) para ponerlo en los
 * correos que llegan por la cola. Cachea 10 minutos para no pedirlo en cada envío de un lote.
 * Si no hay logo o user-service no responde, el correo sale con el nombre de la organización.
 */
@Component
public class MarcaCliente {

    private static final Logger log = LoggerFactory.getLogger(MarcaCliente.class);
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final int MAX_BYTES = 300 * 1024;

    private record Entrada(byte[] logo, String tipo, Instant vence) {}

    private final Map<Long, Entrada> cache = new ConcurrentHashMap<>();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    @Value("${app.user-service-url:http://localhost:8082}")
    private String userServiceUrl;

    public Marca marca(Long organizacionId, String nombre) {
        if (organizacionId == null) return nombre == null ? null : Marca.sinLogo(nombre);
        Entrada e = cache.get(organizacionId);
        if (e == null || e.vence().isBefore(Instant.now())) {
            e = descargar(organizacionId);
            cache.put(organizacionId, e);
        }
        return new Marca(nombre, e.logo(), e.tipo());
    }

    private Entrada descargar(Long orgId) {
        Instant vence = Instant.now().plus(TTL);
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(userServiceUrl.replaceAll("/+$", "")
                    + "/api/public/organizaciones/" + orgId + "/logo"))
                .timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<byte[]> r = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            String tipo = r.headers().firstValue("Content-Type").orElse("").split(";")[0].trim();
            if (r.statusCode() == 200 && r.body() != null && r.body().length <= MAX_BYTES
                    && (tipo.equals("image/png") || tipo.equals("image/jpeg")))
                return new Entrada(r.body(), tipo, vence);
            if (r.statusCode() != 404) log.warn("Logo de la organización {}: respuesta {} ({})", orgId, r.statusCode(), tipo);
        } catch (Exception ex) {
            log.warn("No se pudo obtener el logo de la organización {}: {}", orgId, ex.getMessage());
            vence = Instant.now().plus(Duration.ofMinutes(1));   // reintenta pronto si user-service estaba caído
        }
        return new Entrada(null, null, vence);
    }
}
