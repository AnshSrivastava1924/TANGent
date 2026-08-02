package com.tangent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(long id, LocalDate date, String name, String category, BigDecimal amount) {
}
