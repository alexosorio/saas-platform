package com.saas.platform.email;

/** Error de envío de correo (no acopla la librería a excepciones de cada servicio). */
public class EmailException extends RuntimeException {
    public EmailException(String message) { super(message); }
    public EmailException(String message, Throwable cause) { super(message, cause); }
}
