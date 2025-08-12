package com.punto.de.venta.mcp.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    private String id;
    private User user;
    private String alertType;
    private String message;
    private String status;
    private LocalDateTime scheduledAt;
    private String metadata;
    private LocalDateTime createdAt;
}
