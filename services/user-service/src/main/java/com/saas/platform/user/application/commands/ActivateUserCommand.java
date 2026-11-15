package com.saas.platform.user.application.commands;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ActivateUserCommand {
    private Long userId;
}
