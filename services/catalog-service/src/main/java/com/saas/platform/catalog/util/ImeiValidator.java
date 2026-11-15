package com.saas.platform.catalog.util;

import com.saas.platform.catalog.exception.InvalidOperationException;

/** IMEI: exactamente 15 dígitos y dígito de control válido (algoritmo de Luhn). */
public final class ImeiValidator {
    private ImeiValidator() {}

    /** Devuelve el IMEI normalizado (sin espacios ni guiones) o null si viene vacío. */
    public static String validar(String imei, String campo) {
        if (imei == null || imei.isBlank()) return null;
        String n = imei.replaceAll("[\\s-]", "");
        if (!n.matches("\\d+"))
            throw new InvalidOperationException(campo + " solo debe contener números");
        if (n.length() != 15)
            throw new InvalidOperationException(campo + " debe tener exactamente 15 dígitos (tiene " + n.length() + ")");
        if (!luhn(n))
            throw new InvalidOperationException(campo + " no es válido: el dígito de verificación no coincide. Revisa que esté bien digitado.");
        return n;
    }

    static boolean luhn(String n) {
        int suma = 0;
        for (int i = 0; i < n.length(); i++) {
            int d = n.charAt(n.length() - 1 - i) - '0';
            if (i % 2 == 1) { d *= 2; if (d > 9) d -= 9; }
            suma += d;
        }
        return suma % 10 == 0;
    }
}
