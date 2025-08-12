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
public class Budget {
    private String id;
    private User user;
    private String category;
    private BigDecimal amountLimit;
    private String period;
    private LocalDate startDate;
    private LocalDate endDate;
    private String metadata;
    private LocalDateTime createdAt;
}
