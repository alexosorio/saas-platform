package com.saas.platform.catalog.service;

import com.saas.platform.catalog.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Sube imagenes al disco local del servidor y devuelve una URL relativa
 * servible por el static resource handler de Spring.
 *
 * Archivos guardados en: ${app.uploads.dir}/products/YYYY/MM/<uuid>.<ext>
 * Servidos en:           /uploads/products/YYYY/MM/<uuid>.<ext>
 */
@Service
@Slf4j
public class UploadService {

    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp", "image/gif", "image/svg+xml");
    private static final long MAX_BYTES = 5L * 1024 * 1024;   // 5 MB

    @Value("${app.uploads.dir:./uploads}")
    private String uploadsDir;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(uploadsDir, "products"));
        log.info("Uploads dir: {}", Paths.get(uploadsDir).toAbsolutePath());
    }

    public String uploadProductImage(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new BusinessException("Archivo vacio");
        if (file.getSize() > MAX_BYTES)
            throw new BusinessException("Archivo demasiado grande (max 5MB)");
        String ct = file.getContentType();
        if (ct == null || !ALLOWED.contains(ct))
            throw new BusinessException("Tipo de archivo no permitido: " + ct + ". Acepta JPG, PNG, WEBP, GIF, SVG.");

        try {
            LocalDate today = LocalDate.now();
            String year = String.valueOf(today.getYear());
            String month = String.format("%02d", today.getMonthValue());

            Path dir = Paths.get(uploadsDir, "products", year, month);
            Files.createDirectories(dir);

            String ext = extractExtension(file.getOriginalFilename(), ct);
            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = dir.resolve(fileName);

            // SVG: sanitizar (defensa en profundidad) antes de guardar.
            if ("image/svg+xml".equals(ct) || ".svg".equals(ext)) {
                String svg = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
                Files.writeString(target, sanitizeSvg(svg), java.nio.charset.StandardCharsets.UTF_8);
            } else {
                file.transferTo(target.toAbsolutePath());
            }

            // URL servida por el static handler
            String relativeUrl = "/uploads/products/" + year + "/" + month + "/" + fileName;
            log.info("Imagen guardada: {}", relativeUrl);
            return relativeUrl;
        } catch (IOException e) {
            log.error("Error guardando archivo", e);
            throw new BusinessException("No se pudo guardar el archivo: " + e.getMessage());
        }
    }

    /** Sanitiza SVG: elimina vectores de XSS (best-effort). El render vía <img> ya neutraliza scripts;
     *  esto protege además si el archivo se abre/incrusta directamente. */
    private String sanitizeSvg(String svg) {
        if (svg == null) return "";
        String s = svg;
        s = s.replaceAll("(?is)<!DOCTYPE[^>]*(\\[[^\\]]*\\])?[^>]*>", "");     // DTD / entidades
        s = s.replaceAll("(?is)<script.*?</script>", "");
        s = s.replaceAll("(?is)<script[^>]*/>", "");
        s = s.replaceAll("(?is)<foreignObject.*?</foreignObject>", "");
        s = s.replaceAll("(?is)\\son[a-z]+\\s*=\\s*\"[^\"]*\"", "");           // onload, onclick, ...
        s = s.replaceAll("(?is)\\son[a-z]+\\s*=\\s*'[^']*'", "");
        s = s.replaceAll("(?is)\\son[a-z]+\\s*=\\s*[^\\s>]+", "");
        s = s.replaceAll("(?is)(href|xlink:href|src)\\s*=\\s*\"\\s*javascript:[^\"]*\"", "$1=\"#\"");
        s = s.replaceAll("(?is)(href|xlink:href|src)\\s*=\\s*'\\s*javascript:[^']*'", "$1='#'");
        s = s.replaceAll("(?is)<(iframe|embed|object|link|meta|base)\\b[^>]*>.*?</\\1>", "");
        s = s.replaceAll("(?is)<(iframe|embed|object|link|meta|base)\\b[^>]*/?>", "");
        return s;
    }

    private String extractExtension(String original, String contentType) {
        if (original != null) {
            int dot = original.lastIndexOf('.');
            if (dot > 0 && dot < original.length() - 1) {
                String ext = original.substring(dot).toLowerCase();
                if (ext.matches("\\.(jpg|jpeg|png|webp|gif|svg)")) return ext;
            }
        }
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif"  -> ".gif";
            case "image/svg+xml" -> ".svg";
            default           -> ".bin";
        };
    }
}
