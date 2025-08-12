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
public class Loan {
    private String id;
    private User user;
    private BigDecimal principal;
    private BigDecimal interestRate;
    private BigDecimal monthlyPayment;
    private LocalDate startDate;
    private Integer paymentDay;
    private String description;
    private String metadata;
    private LocalDateTime createdAt;
}
