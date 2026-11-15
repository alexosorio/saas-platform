package com.saas.platform.catalog.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class CreateCaracteristicaRequest {
    @NotBlank @Size(max = 100) private String nombre;
    @NotBlank @Size(max = 50) private String codigo;
    private String tipoDato;     // texto, numero, fecha, booleano, lista
    private String opcionesLista;
    private Boolean obligatorio;
    private String aplicableA;   // moviles, accesorios, todos
}
