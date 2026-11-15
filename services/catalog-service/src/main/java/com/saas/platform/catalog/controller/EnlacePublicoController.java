package com.saas.platform.catalog.controller;

import com.saas.platform.catalog.dto.CreateEnlacePublicoRequest;
import com.saas.platform.catalog.dto.EnlacePublicoDTO;
import com.saas.platform.catalog.dto.EnviarEnlaceRequest;
import com.saas.platform.catalog.dto.EnvioMasivoRequest;
import com.saas.platform.catalog.dto.EnvioMasivoResultDTO;
import com.saas.platform.catalog.dto.MessageResponse;
import com.saas.platform.catalog.security.UserPrincipal;
import com.saas.platform.catalog.service.EnlacePublicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lists/{listaId}/links")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class EnlacePublicoController {

    private final EnlacePublicoService service;

    @GetMapping
    public ResponseEntity<List<EnlacePublicoDTO>> getAll(@PathVariable Long listaId,
                                                        @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.listarPorLista(listaId, user.getOrganizacionId()));
    }

    @PostMapping
    public ResponseEntity<EnlacePublicoDTO> create(@PathVariable Long listaId,
                                                   @RequestBody CreateEnlacePublicoRequest req,
                                                   @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            service.crear(listaId, req, user.getOrganizacionId(), user.getId()));
    }

    @PutMapping("/{enlaceId}/deactivate")
    public ResponseEntity<EnlacePublicoDTO> deactivate(@PathVariable Long enlaceId,
                                                       @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.desactivar(enlaceId, user.getOrganizacionId()));
    }

    @DeleteMapping("/{enlaceId}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long enlaceId,
                                                  @AuthenticationPrincipal UserPrincipal user) {
        service.eliminar(enlaceId, user.getOrganizacionId());
        return ResponseEntity.ok(MessageResponse.builder().message("Enlace eliminado").success(true).build());
    }

    /** Envía el enlace por correo (SendGrid) desde el backend. */
    @PostMapping("/{enlaceId}/send-email")
    public ResponseEntity<MessageResponse> sendEmail(@PathVariable Long enlaceId,
                                                     @RequestBody EnviarEnlaceRequest req,
                                                     @AuthenticationPrincipal UserPrincipal user) {
        service.enviarPorEmail(enlaceId, user.getOrganizacionId(), req.getDestinatario(), req.getMensaje());
        return ResponseEntity.ok(MessageResponse.builder().message("Correo enviado").success(true).build());
    }

    /** Envía el enlace por WhatsApp (Cloud API de Meta) desde el backend. */
    @PostMapping("/{enlaceId}/send-whatsapp")
    public ResponseEntity<MessageResponse> sendWhatsApp(@PathVariable Long enlaceId,
                                                        @RequestBody EnviarEnlaceRequest req,
                                                        @AuthenticationPrincipal UserPrincipal user) {
        service.enviarPorWhatsApp(enlaceId, user.getOrganizacionId(), req.getTelefono(), req.getMensaje());
        return ResponseEntity.ok(MessageResponse.builder().message("WhatsApp enviado").success(true).build());
    }

    /** Envío masivo a clientes seleccionados (correo y/o WhatsApp). */
    @PostMapping("/{enlaceId}/send-bulk")
    public ResponseEntity<EnvioMasivoResultDTO> sendBulk(@PathVariable Long enlaceId,
                                                         @RequestBody EnvioMasivoRequest req,
                                                         @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(service.enviarMasivo(enlaceId, user.getOrganizacionId(), req));
    }
}
