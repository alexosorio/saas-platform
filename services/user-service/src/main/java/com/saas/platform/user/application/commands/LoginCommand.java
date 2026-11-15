package com.saas.platform.user.application.commands;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LoginCommand {
    private String email;
    private String password;
}
