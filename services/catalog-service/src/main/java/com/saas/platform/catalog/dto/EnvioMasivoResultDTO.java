package com.saas.platform.catalog.dto;
import lombok.*;
import java.util.ArrayList;
import java.util.List;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EnvioMasivoResultDTO {
    @Builder.Default private int enviados = 0;
    @Builder.Default private int omitidos = 0;
    @Builder.Default private int fallidos = 0;
    @Builder.Default private List<Item> detalle = new ArrayList<>();

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Item {
        private Long clienteId;
        private String nombre;
        private String canal;     // email | whatsapp
        private String destino;   // correo o teléfono usado
        private String estado;    // ENVIADO | OMITIDO | FALLIDO
        private String motivo;    // detalle en omitido/fallido
    }
    public void add(Item i) {
        detalle.add(i);
        switch (i.getEstado()) {
            case "ENVIADO", "ENCOLADO" -> enviados++;
            case "OMITIDO" -> omitidos++;
            default -> fallidos++;
        }
    }
}
