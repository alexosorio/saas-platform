package com.saas.platform.user.application.commands;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ResetPasswordWithTokenCommand {
    private String token;
    private String newPassword;
}
