package fr.uge.project.ruge.strategy.impl;

import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.services.ConfigurationService;
import fr.uge.project.ruge.strategy.SuperLikeImpact;
import fr.uge.project.ruge.strategy.SuperLikePricingStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class StandardSuperLikeStrategy implements SuperLikePricingStrategy {

    private static final BigDecimal SITE_RATIO = new BigDecimal("0.40");
    private static final BigDecimal DISTRIBUTE_RATIO = new BigDecimal("0.60");

    private final ConfigurationService configurationService;

    public StandardSuperLikeStrategy(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public SuperLikeImpact calculateImpact(UserEntity user, int quantity) {
        // Récupération du prix dynamique depuis la BDD
        BigDecimal price = configurationService.getConfiguration().getSuperLikePrice();
        BigDecimal total = price.multiply(BigDecimal.valueOf(quantity));

        return new SuperLikeImpact(
                total,
                total.multiply(SITE_RATIO),
                total.multiply(DISTRIBUTE_RATIO),
                0,
                "Tarif standard : " + price + "€",
                LocalDateTime.now(),
                null
        );
    }
}