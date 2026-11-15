package com.saas.platform.user.application.queries;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GetAccessibleModulesQuery {
    private Long userId;
    private Long organizacionId;
}
