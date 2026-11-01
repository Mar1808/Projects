package fr.uge.project.ruge.strategy;

import fr.uge.project.ruge.entity.PromotionEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SuperLikeImpact(
    BigDecimal priceToPay,         // Ce que l'utilisateur paie
    BigDecimal siteBenefShare,     // Les 40% pour le site
    BigDecimal distributionShare,  // Les 60% pour les créateurs
    int freeLikesIncluded,         // Pour la règle des "10+1 gratuit"
    String explanation,            // Description textuelle
    LocalDateTime timestamp,       // Précision temporelle
    PromotionEntity promotion      // La promotion appliquée (null si aucune)
) {}