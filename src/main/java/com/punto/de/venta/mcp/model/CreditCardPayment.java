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
public class CreditCardPayment {
    
    private Long id;
    private Long creditCardId;
    private Long transactionId;
    private LocalDate paymentDate;
    private BigDecimal amountPaid;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
