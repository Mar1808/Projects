package fr.uge.project.ruge.strategy;
import fr.uge.project.ruge.entity.UserEntity;

public interface AiSearchPricingStrategy {
    AiSearchImpact calculateImpact(UserEntity user);
}