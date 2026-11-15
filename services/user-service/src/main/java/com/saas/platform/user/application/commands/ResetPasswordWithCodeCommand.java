package com.saas.platform.user.application.commands;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ResetPasswordWithCodeCommand {
    private String email;
    private String code;
    private String newPassword;
}
