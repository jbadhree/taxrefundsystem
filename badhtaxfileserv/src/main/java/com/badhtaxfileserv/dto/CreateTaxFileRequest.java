package com.badhtaxfileserv.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaxFileRequest {
    
    @NotBlank(message = "User ID is required")
    @Size(max = 100, message = "User ID must not exceed 100 characters")
    private String userId;
    
    @NotNull(message = "Year is required")
    @Min(value = 1900, message = "Year must be at least 1900")
    @Max(value = 2100, message = "Year must be at most 2100")
    private Integer year;
    
    @NotNull(message = "Income is required")
    @DecimalMin(value = "0.0", message = "Income must be non-negative")
    @Digits(integer = 12, fraction = 2, message = "Income must have at most 12 integer digits and 2 decimal places")
    private BigDecimal income;
    
    @NotNull(message = "Expense is required")
    @DecimalMin(value = "0.0", message = "Expense must be non-negative")
    @Digits(integer = 12, fraction = 2, message = "Expense must have at most 12 integer digits and 2 decimal places")
    private BigDecimal expense;
    
    @NotNull(message = "Tax Rate is required")
    @DecimalMin(value = "0.0", message = "Tax Rate must be non-negative")
    @DecimalMax(value = "100.0", message = "Tax Rate must not exceed 100")
    @Digits(integer = 3, fraction = 2, message = "Tax Rate must have at most 3 integer digits and 2 decimal places")
    private BigDecimal taxRate;
    
    @NotNull(message = "Deducted is required")
    @DecimalMin(value = "0.0", message = "Deducted must be non-negative")
    @Digits(integer = 12, fraction = 2, message = "Deducted must have at most 12 integer digits and 2 decimal places")
    private BigDecimal deducted;
    
    @NotNull(message = "Refund is required")
    @DecimalMin(value = "0.0", message = "Refund must be non-negative")
    @Digits(integer = 12, fraction = 2, message = "Refund must have at most 12 integer digits and 2 decimal places")
    private BigDecimal refund;
}

