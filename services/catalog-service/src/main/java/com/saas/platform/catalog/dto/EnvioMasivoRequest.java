package com.saas.platform.catalog.dto;
import lombok.Data;
import java.util.List;
@Data
public class EnvioMasivoRequest {
    private List<Long> clienteIds;   // clientes seleccionados
    private String canal;            // "email" | "whatsapp" | "ambos"
    private String mensaje;          // mensaje opcional que se antepone
}
