package com.portfolio.management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("transactions")
public class Transaction {

    @Id
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
}