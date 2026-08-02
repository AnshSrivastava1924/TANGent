package com.portfolio.management.repository;

import com.portfolio.management.model.PortfolioAsset;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioAssetRepository extends CrudRepository<PortfolioAsset, Long> {

    List<PortfolioAsset> findByPortfolioId(Long portfolioId);

    Optional<PortfolioAsset> findByAssetId(Long assetId);

    @Query("""
            SELECT *
            FROM portfolio_assets
            WHERE asset_id = :assetId
              AND portfolio_id = :portfolioId
            """)
    Optional<PortfolioAsset> findByAssetIdAndPortfolioId(Long assetId, Long portfolioId);
}