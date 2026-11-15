package com.saas.platform.user.application.commands.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandResult {
    private boolean success;
    private String message;
    private Object data;
    
    public static CommandResult success(String message) {
        return CommandResult.builder()
                .success(true)
                .message(message)
                .build();
    }
    
    public static CommandResult success(String message, Object data) {
        return CommandResult.builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }
    
    public static CommandResult failure(String message) {
        return CommandResult.builder()
                .success(false)
                .message(message)
                .build();
    }
}
