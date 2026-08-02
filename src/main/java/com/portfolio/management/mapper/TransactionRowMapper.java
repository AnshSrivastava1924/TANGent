package com.portfolio.management.mapper;

import com.portfolio.management.model.Transaction;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class TransactionRowMapper implements RowMapper<Transaction> {

    @Override
    public Transaction mapRow(ResultSet rs, int rowNum) throws SQLException {

        return Transaction.builder()
                .transactionId(rs.getLong("transaction_id"))
                .assetId(rs.getLong("asset_id"))
                .transactionType(rs.getString("transaction_type"))
                .quantity(rs.getBigDecimal("quantity"))
                .pricePerUnit(rs.getBigDecimal("price_per_unit"))
                .totalAmount(rs.getBigDecimal("total_amount"))
                .transactionDate(rs.getTimestamp("transaction_date").toLocalDateTime())
                .notes(rs.getString("notes"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                .build();
    }
}