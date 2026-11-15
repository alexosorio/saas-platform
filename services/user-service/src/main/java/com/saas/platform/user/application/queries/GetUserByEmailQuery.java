package com.saas.platform.user.application.queries;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GetUserByEmailQuery {
    private String email;
}
