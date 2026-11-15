package com.saas.platform.user.application.commands;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RequestPasswordRecoveryCommand {
    private String email;
}
