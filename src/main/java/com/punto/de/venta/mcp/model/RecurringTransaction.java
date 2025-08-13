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
public class RecurringTransaction {
    private Long id;
    private User user;
    private String type;
    private String description;
    private BigDecimal amount;
    private String frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal interestRate;
    private String category;
    private String metadata;
    private LocalDateTime createdAt;
}
