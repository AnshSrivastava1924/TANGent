package com.portfolio.management.repository;

import com.portfolio.management.model.Portfolio;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends CrudRepository<Portfolio, Long> {

    /**
     * Get all portfolios of a user
     */
    @Query("""
            SELECT *
            FROM portfolios
            WHERE user_id = :userId
            ORDER BY created_at DESC
            """)
    List<Portfolio> findByUserId(Long userId);

    /**
     * Find portfolio by ID and user
     */
    @Query("""
            SELECT *
            FROM portfolios
            WHERE portfolio_id = :portfolioId
              AND user_id = :userId
            """)
    Optional<Portfolio> findByPortfolioIdAndUserId(Long portfolioId, Long userId);

    /**
     * Check whether portfolio exists for a user
     */
    boolean existsByPortfolioIdAndUserId(Long portfolioId, Long userId);
}