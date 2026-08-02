package com.portfolio.management.repository;

import com.portfolio.management.model.Watchlist;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistRepository extends CrudRepository<Watchlist, Long> {

    List<Watchlist> findByUserId(Long userId);

    Optional<Watchlist> findByWatchlistId(Long watchlistId);

    @Query("""
            SELECT *
            FROM watchlists
            WHERE user_id = :userId
              AND asset_id = :assetId
            """)
    Optional<Watchlist> findByUserIdAndAssetId(Long userId, Long assetId);
}