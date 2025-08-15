package com.punto.de.venta.mcp.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCategory {

    private Long id;
    private User user;
    private String categoryName;
    private TransactionCategory parentCategory;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
