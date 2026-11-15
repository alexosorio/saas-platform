package com.saas.platform.user.service;

import com.saas.platform.email.EmailSender;
import com.saas.platform.email.Marca;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.HashMap;
import java.util.Map;

/**
 * Correos del servicio de usuarios, con la marca (logo y nombre) de la organización del destinatario.
 *
 * Cada método arma el correo en el hilo que lo pide (dentro de su transacción: ve la organización y
 * el logo recién creados) y lo ENVÍA en segundo plano DESPUÉS del commit: si la operación falla y se
 * revierte, no sale un correo con un enlace que no existe.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailSender emailSender;
    private final SpringTemplateEngine templateEngine;
    private final MarcaService marcaService;
    /** Pool definido en AppConfig ("taskExecutor"). */
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.beans.factory.annotation.Qualifier("taskExecutor")
    private java.util.concurrent.Executor taskExecutor;

    @Value("${app.url}")
    private String appUrl;

    /** Verificación de correo: enlace (principal) y código de respaldo. */
    public void enviarVerificacion(String email, String nombre, String enlace, int validezHoras,
                                   String codigo, int validezMinutosCodigo) {
        Map<String, Object> v = new HashMap<>();
        v.put("nombre", nombre);
        v.put("enlace", enlace);
        v.put("validezHoras", validezHoras);
        v.put("codigo", codigo);
        v.put("validezMinutos", validezMinutosCodigo);
        enviar(email, "Confirma tu correo para activar tu cuenta", "Confirma tu correo", "email-verificacion", v);
    }

    /** Código de 6 dígitos (flujo anterior; se conserva por compatibilidad). */
    public void enviarCodigoVerificacion(String email, String nombre, String codigo) {
        Map<String, Object> v = new HashMap<>();
        v.put("nombre", nombre);
        v.put("codigo", codigo);
        v.put("validezMinutos", 30);
        v.put("enlace", appUrl + "/verificar-email");
        v.put("validezHoras", 0);
        enviar(email, "Verifica tu cuenta - Código de verificación", "Confirma tu correo", "email-verificacion", v);
    }

    public void enviarCodigoRecuperacion(String email, String nombre, String codigo) {
        Map<String, Object> v = new HashMap<>();
        v.put("nombre", nombre);
        v.put("codigo", codigo);
        v.put("validezMinutos", 15);
        enviar(email, "Recuperación de contraseña - Código de seguridad", "Recupera tu contraseña", "codigo-recuperacion", v);
    }

    public void enviarEnlaceRecuperacion(String email, String nombre, String token) {
        Map<String, Object> v = new HashMap<>();
        v.put("nombre", nombre);
        v.put("resetLink", appUrl + "/reset-password?token=" + token);
        v.put("validezMinutos", 60);
        enviar(email, "Recuperación de contraseña - Enlace seguro", "Restablece tu contraseña", "enlace-recuperacion", v);
    }

    public void enviarNotificacionCambioPassword(String email, String nombre) {
        Map<String, Object> v = new HashMap<>();
        v.put("nombre", nombre);
        enviar(email, "Tu contraseña ha sido cambiada", "Contraseña actualizada", "cambio-password", v);
    }

    public void enviarBienvenida(String email, String nombre) {
        Map<String, Object> v = new HashMap<>();
        v.put("nombre", nombre);
        v.put("appUrl", appUrl);
        enviar(email, "¡Bienvenido! Tu cuenta está activa", "¡Bienvenido!", "bienvenida", v);
    }

    // ── Núcleo ──────────────────────────────────────────────────────────

    private void enviar(String email, String asunto, String titulo, String plantilla, Map<String, Object> variables) {
        Marca marca;
        String contenido;
        try {
            marca = marcaService.marcaDeUsuario(email);
            variables.put("orgNombre", marca != null ? marca.nombre() : null);
            Context ctx = new Context();
            ctx.setVariables(variables);
            contenido = templateEngine.process(plantilla, ctx);
        } catch (Exception e) {
            log.error("No se pudo preparar el correo '{}' para {}: {}", plantilla, email, e.getMessage());
            return;
        }
        Runnable envio = () -> {
            try {
                emailSender.enviar(email, asunto, titulo, contenido, marca);
                log.info("Correo '{}' enviado a {}", plantilla, email);
            } catch (Exception e) {
                log.error("Error enviando correo '{}' a {}: {}", plantilla, email, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { taskExecutor.execute(envio); }
            });
        } else {
            taskExecutor.execute(envio);
        }
    }
}
