package fr.uge.project.ruge.strategy;
import fr.uge.project.ruge.entity.UserEntity;

public interface SuperLikePricingStrategy {
    SuperLikeImpact calculateImpact(UserEntity user, int quantity);
}