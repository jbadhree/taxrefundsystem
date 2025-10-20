package com.badhtaxfileserv.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "refund", schema = "taxfileservdb")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Refund {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_file_id", nullable = false)
    private TaxFile taxFile;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", nullable = false, length = 32)
    private RefundStatus refundStatus;
    
    @Column(name = "refund_errors", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String refundErrors;
    
    @Column(name = "refund_eta")
    private LocalDateTime refundEta;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RefundEvent> events = new ArrayList<>();
    
    public enum RefundStatus {
        PENDING, IN_PROGRESS, APPROVED, REJECTED, ERROR
    }
}

