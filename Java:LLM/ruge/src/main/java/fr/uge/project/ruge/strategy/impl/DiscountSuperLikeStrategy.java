package fr.uge.project.ruge.strategy.impl;

import fr.uge.project.ruge.entity.PromotionEntity;
import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.strategy.SuperLikeImpact;
import fr.uge.project.ruge.strategy.SuperLikePricingStrategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DiscountSuperLikeStrategy implements SuperLikePricingStrategy {

    private static final BigDecimal USER_AMOUNT = new BigDecimal("0.40");
    private static final BigDecimal APP_AMOUNT = new BigDecimal("0.60");
    private final PromotionEntity promo;

    public DiscountSuperLikeStrategy(PromotionEntity promo) {
        this.promo = promo;
    }

    @Override
    public SuperLikeImpact calculateImpact(UserEntity user, int quantity) {
        BigDecimal total = promo.getPromoPrice().multiply(BigDecimal.valueOf(quantity));
        return new SuperLikeImpact(
            total,
            total.multiply(USER_AMOUNT),
            total.multiply(APP_AMOUNT),
            0,
            "Promotion appliquée : " + promo.getDescription(),
            LocalDateTime.now(),
            promo
        );
    }
}