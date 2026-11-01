package fr.uge.project.ruge.strategy;
import java.math.BigDecimal;

public record AiSearchImpact(
    BigDecimal priceToPay,
    String explanation
) {}