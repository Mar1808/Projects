package fr.uge.project.ruge.strategy.impl;

import fr.uge.project.ruge.entity.PromotionEntity;
import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.strategy.AiSearchImpact;
import fr.uge.project.ruge.strategy.AiSearchPricingStrategy;
import java.math.BigDecimal;

public class DiscountAiSearchStrategy implements AiSearchPricingStrategy {
    
    private final PromotionEntity promotion;

    public DiscountAiSearchStrategy(PromotionEntity promotion) {
        this.promotion = promotion;
    }

    @Override
    public AiSearchImpact calculateImpact(UserEntity user) {
        BigDecimal reducedPrice = promotion.getPromoPrice();
        return new AiSearchImpact(reducedPrice, "Promo IA active");
    }
}