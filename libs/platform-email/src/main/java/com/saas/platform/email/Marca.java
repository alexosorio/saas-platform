package com.saas.platform.email;

/**
 * Identidad de la organización que firma un correo: nombre y logo opcional.
 * El logo viaja como imagen INLINE (Content-ID {@link #CID_LOGO}): se ve sin depender de
 * una URL pública y sin que el cliente de correo bloquee imágenes remotas.
 * Debe ser PNG o JPEG: Gmail y Outlook no muestran SVG.
 */
public record Marca(String nombre, byte[] logo, String logoTipo) {

    public static final String CID_LOGO = "logo-org";

    public static Marca sinLogo(String nombre) { return new Marca(nombre, null, null); }

    public boolean tieneLogo() {
        return logo != null && logo.length > 0 && logoTipo != null && logoTipo.startsWith("image/");
    }
}
