package com.saas.platform.user.application.queries;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GetUsersByOrganizationQuery {
    private Long organizacionId;
    private Integer page = 0;
    private Integer size = 20;
}
