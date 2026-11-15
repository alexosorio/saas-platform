package com.saas.platform.user.application.commands;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AssignRolToUserCommand {
    private Long userId;
    private Long rolId;
}
