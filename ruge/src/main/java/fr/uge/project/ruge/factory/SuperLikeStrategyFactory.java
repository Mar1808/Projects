package fr.uge.project.ruge.factory;

import fr.uge.project.ruge.entity.PromotionEntity;
import fr.uge.project.ruge.repositories.PromotionRepository;
import fr.uge.project.ruge.repositories.SuperLikeRepository;
import fr.uge.project.ruge.services.ConfigurationService;
import fr.uge.project.ruge.strategy.SuperLikePricingStrategy;
import fr.uge.project.ruge.strategy.impl.DiscountSuperLikeStrategy;
import fr.uge.project.ruge.strategy.impl.FreeLikeSuperLikeStrategy;
import fr.uge.project.ruge.strategy.impl.StandardSuperLikeStrategy;
import org.springframework.stereotype.Component;

@Component
public class SuperLikeStrategyFactory {
  private static final String SUPERLIKE_PROMOTION_TYPE = "SUPERLIKE";
  private static final String FREE_LIKE_PROMOTION_TYPE = "FREE_LIKE";

  private final StandardSuperLikeStrategy standardStrategy;
  private final PromotionRepository promotionRepository;
  private final SuperLikeRepository superLikeRepository;
  private final ConfigurationService configurationService;

  public SuperLikeStrategyFactory(StandardSuperLikeStrategy standard,
                                  PromotionRepository repo,
                                  SuperLikeRepository superLikeRepository,
                                  ConfigurationService configurationService) {
    this.standardStrategy = standard;
    this.promotionRepository = repo;
    this.superLikeRepository = superLikeRepository;
    this.configurationService = configurationService;
  }

  public SuperLikePricingStrategy getStrategy() {
    var activePromoOpt = promotionRepository.findAll().stream()
        .filter(PromotionEntity::isActive)
        .filter(this::isSuperLikePromotion)
        .findFirst();

    if (activePromoOpt.isPresent()) {
      var promo = activePromoOpt.get();
      if (FREE_LIKE_PROMOTION_TYPE.equalsIgnoreCase(promo.getType())) {
        var freeLikeInterval = promo.getPromoPrice().intValue();
        return new FreeLikeSuperLikeStrategy(promo, superLikeRepository, configurationService, freeLikeInterval);
      }
      return new DiscountSuperLikeStrategy(promo);
    }
    return standardStrategy;
  }

  private boolean isSuperLikePromotion(PromotionEntity promotion) {
    var type = promotion.getType();
    if (type == null) {
      return false;
    }
    return SUPERLIKE_PROMOTION_TYPE.equalsIgnoreCase(type)
        || FREE_LIKE_PROMOTION_TYPE.equalsIgnoreCase(type);
  }
}