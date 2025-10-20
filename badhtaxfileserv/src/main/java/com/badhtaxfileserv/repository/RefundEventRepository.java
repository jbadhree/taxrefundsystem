package com.badhtaxfileserv.repository;

import com.badhtaxfileserv.entity.RefundEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RefundEventRepository extends JpaRepository<RefundEvent, UUID> {
    
    List<RefundEvent> findByRefundIdOrderByEventDateAsc(UUID refundId);
}

