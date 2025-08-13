package com.punto.de.venta.mcp.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditCard {
    
    private Long id;
    private Long userId;
    private String cardName;
    private String lastFourDigits;
    private Integer cutOffDay;
    private Integer paymentDueDay;
    private BigDecimal creditLimit;
    private BigDecimal currentBalance;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
