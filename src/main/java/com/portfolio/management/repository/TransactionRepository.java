package com.portfolio.management.repository;

import com.portfolio.management.model.Transaction;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends CrudRepository<Transaction, Long> {

    @Query("SELECT * FROM transactions WHERE asset_id = :assetId")
    List<Transaction> findByAssetId(Long assetId);
}