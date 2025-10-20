package com.badhtaxfileserv.repository;

import com.badhtaxfileserv.entity.TaxFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxFileRepository extends JpaRepository<TaxFile, UUID> {
    
    @Query("SELECT tf FROM TaxFile tf LEFT JOIN FETCH tf.refund WHERE tf.userId = :userId AND tf.year = :year")
    Optional<TaxFile> findByUserIdAndYearWithRefund(@Param("userId") String userId, @Param("year") Integer year);
    
    @Query("SELECT tf FROM TaxFile tf LEFT JOIN FETCH tf.refund WHERE tf.userId = :userId ORDER BY tf.year DESC")
    List<TaxFile> findByUserIdWithRefund(@Param("userId") String userId);
    
    boolean existsByUserIdAndYear(String userId, Integer year);
}

