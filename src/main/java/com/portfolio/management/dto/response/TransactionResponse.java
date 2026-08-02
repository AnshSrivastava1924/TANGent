package com.portfolio.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private Long transactionId;

    private Long assetId;

    private String transactionType;

    private BigDecimal quantity;

    private BigDecimal pricePerUnit;

    private BigDecimal totalAmount;

    private LocalDateTime transactionDate;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String message;
}
