package com.saas.platform.user.application.commands;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VerifyEmailCommand {
    private String email;
    private String code;
}
