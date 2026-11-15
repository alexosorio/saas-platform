package com.saas.platform.subscription.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;
@Data
public class CreateMetodoPagoRequest {
    @NotNull private String tipo;     // tarjeta_credito, tarjeta_debito, transferencia, paypal, stripe
    @Size(max = 50) private String proveedor;
    @Size(max = 4) private String ultimosDigitos;
    private String token;
    @Size(max = 200) private String nombreTitular;
    private LocalDate fechaExpiracion;
    private Boolean predeterminado;
    // Habeas Data
    private Boolean autorizacionDatos;
    private java.time.LocalDateTime fechaAutorizacion;
    private String versionPolitica;
}
