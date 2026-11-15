package com.saas.platform.catalog.dto;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MessageResponse {
    private String message;
    private Boolean success;
    private Object data;
}
