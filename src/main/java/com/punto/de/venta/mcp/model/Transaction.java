package com.punto.de.venta.mcp.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private Long id;
    private User user;
    private String type;
    private String description;
    private String category;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String recurringRef;
    private String creditCardId;
    private String source;
    private String metadata;
    private LocalDateTime createdAt;
}
