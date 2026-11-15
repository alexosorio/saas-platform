package com.saas.platform.catalog.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class CreateClienteRequest {
    @NotBlank @Size(max = 255) private String nombre;
    private String tipo;   // mayorista, minorista
    @Size(max = 50) private String documento;
    @Email private String email;
    private String telefono;
    private String direccion;
    private Long ciudadId;
    private BigDecimal limiteCredito;
}
