package com.saas.platform.subscription.dto;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MessageResponse { private String message; private Boolean success; private Object data; }
