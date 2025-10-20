package com.badhtaxfileserv.repository;

import com.badhtaxfileserv.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {
    
    @Query("SELECT r FROM Refund r JOIN r.taxFile tf WHERE tf.userId = :userId AND tf.year = :year")
    Optional<Refund> findByUserIdAndYear(@Param("userId") String userId, @Param("year") Integer year);
    
    Optional<Refund> findByTaxFileId(UUID taxFileId);
}

