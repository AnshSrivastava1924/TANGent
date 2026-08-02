package com.portfolio.management.repository;

import com.portfolio.management.model.User;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if email already exists
     */
    boolean existsByEmail(String email);

    /**
     * Find active user by email
     */
    @Query("""
            SELECT *
            FROM users
            WHERE email = :email
              AND is_active = true
            """)
    Optional<User> findActiveUserByEmail(@Param("email") String email);
}