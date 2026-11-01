package fr.uge.project.ruge.strategy.impl;

import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.services.ConfigurationService;
import fr.uge.project.ruge.strategy.AiSearchImpact;
import fr.uge.project.ruge.strategy.AiSearchPricingStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class StandardAiSearchStrategy implements AiSearchPricingStrategy {

    private final ConfigurationService configurationService;

    public StandardAiSearchStrategy(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public AiSearchImpact calculateImpact(UserEntity user) {
        // Récupération du prix dynamique de la recherche IA
        BigDecimal basePrice = configurationService.getConfiguration().getAiSearchPrice();
        return new AiSearchImpact(basePrice, "Recherche IA standard : " + basePrice + "€");
    }
}