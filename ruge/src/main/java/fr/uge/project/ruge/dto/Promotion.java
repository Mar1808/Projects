package fr.uge.project.ruge.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Promotion(
        Long id,
        String type,
        BigDecimal promoPrice,
        LocalDateTime startDate,
        LocalDateTime endDate
) {
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return startDate != null && endDate != null
                && !now.isBefore(startDate)
                && !now.isAfter(endDate);
    }
}