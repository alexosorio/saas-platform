package com.saas.platform.catalog.dto;
import lombok.*;
import java.util.List;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CargaMasivaResultDTO {
    private int productosCreados;
    private int productosExistentes;   // SKU ya existia, se le agregaron unidades
    private int unidadesCreadas;
    private int filasConError;
    private List<String> errores;      // "Fila 5: ..." 
}
