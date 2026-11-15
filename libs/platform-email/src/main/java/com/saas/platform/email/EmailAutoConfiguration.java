package com.saas.platform.email;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/** Registra EmailLayout y EmailSender automáticamente en cualquier servicio que incluya la librería. */
@AutoConfiguration
public class EmailAutoConfiguration {

    @Bean
    public EmailLayout emailLayout() { return new EmailLayout(); }

    @Bean
    public EmailSender emailSender(EmailLayout emailLayout) { return new EmailSender(emailLayout); }
}
