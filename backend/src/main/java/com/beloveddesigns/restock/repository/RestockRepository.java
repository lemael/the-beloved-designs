package com.beloveddesigns.restock.repository;

import com.beloveddesigns.restock.entity.Restock;
import com.beloveddesigns.restock.entity.RestockStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RestockRepository extends JpaRepository<Restock, Long> {

    Page<Restock> findByStatus(RestockStatus status, Pageable pageable);

    long countByStatus(RestockStatus status);

    /** Verrou ligne : empêche deux réceptions simultanées du même ravitaillement (double incrément du stock). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Restock r where r.id = :id")
    Optional<Restock> findByIdForUpdate(@Param("id") Long id);
}
