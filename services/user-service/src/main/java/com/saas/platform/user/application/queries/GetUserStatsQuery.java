package com.saas.platform.user.application.queries;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GetUserStatsQuery {
    private Long userId;
}
