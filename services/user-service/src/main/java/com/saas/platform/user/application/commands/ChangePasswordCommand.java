package com.saas.platform.user.application.commands;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ChangePasswordCommand {
    private Long userId;
    private String currentPassword;
    private String newPassword;
}
