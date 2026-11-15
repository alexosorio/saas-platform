package com.saas.platform.user.application.queries;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class GetAllUsersQuery {
    private Integer page = 0;
    private Integer size = 20;
}
