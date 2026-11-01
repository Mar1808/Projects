package fr.uge.project.ruge.factory;

import fr.uge.project.ruge.repositories.PromotionRepository;
import fr.uge.project.ruge.strategy.AiSearchPricingStrategy;
import fr.uge.project.ruge.strategy.impl.DiscountAiSearchStrategy;
import fr.uge.project.ruge.strategy.impl.StandardAiSearchStrategy;
import org.springframework.stereotype.Component;

@Component
public class AiSearchStrategyFactory {

    private final StandardAiSearchStrategy standardStrategy;
    private final PromotionRepository promotionRepository;

    public AiSearchStrategyFactory(StandardAiSearchStrategy standardStrategy, PromotionRepository promotionRepository) {
        this.standardStrategy = standardStrategy;
        this.promotionRepository = promotionRepository;
    }

    public AiSearchPricingStrategy getStrategy() {
        var activeAiPromo = promotionRepository.findAll().stream()
                .filter(p -> p.isActive() && "PROMO_AI".equalsIgnoreCase(p.getType()))
                .findFirst();
        if (activeAiPromo.isPresent()) {
            return new DiscountAiSearchStrategy(activeAiPromo.get());
        }
        return standardStrategy;
    }
}