package com.badhtaxfileserv.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tax_file", schema = "taxfileservdb")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxFile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;
    
    @Column(name = "tax_year", nullable = false)
    private Integer year;
    
    @Column(name = "income", nullable = false, precision = 14, scale = 2)
    private BigDecimal income;
    
    @Column(name = "expense", nullable = false, precision = 14, scale = 2)
    private BigDecimal expense;
    
    @Column(name = "tax_rate_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;
    
    @Column(name = "deducted", nullable = false, precision = 14, scale = 2)
    private BigDecimal deducted;
    
    @Column(name = "refund_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal refundAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tax_status", nullable = false, length = 32)
    private TaxStatus taxStatus;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToOne(mappedBy = "taxFile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Refund refund;
    
    public enum TaxStatus {
        PENDING, COMPLETED
    }
}

