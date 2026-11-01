package fr.uge.project.ruge.strategy.impl;

import fr.uge.project.ruge.entity.PromotionEntity;
import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.repositories.SuperLikeRepository;
import fr.uge.project.ruge.services.ConfigurationService;
import fr.uge.project.ruge.strategy.SuperLikeImpact;
import fr.uge.project.ruge.strategy.SuperLikePricingStrategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class FreeLikeSuperLikeStrategy implements SuperLikePricingStrategy {
  private static final String FREE_LIKE_PROMOTION_TYPE = "FREE_LIKE";
  private static final BigDecimal SITE_SHARE_RATIO = new BigDecimal("0.40");
  private static final BigDecimal DISTRIBUTION_SHARE_RATIO = new BigDecimal("0.60");

  private final PromotionEntity promo;
  private final SuperLikeRepository superLikeRepository;
  private final ConfigurationService configurationService;
  private final int freeLikeInterval;

  public FreeLikeSuperLikeStrategy(PromotionEntity promo,
                                   SuperLikeRepository superLikeRepository,
                                   ConfigurationService configurationService,
                                   int freeLikeInterval) {
    this.promo = Objects.requireNonNull(promo);
    this.superLikeRepository = Objects.requireNonNull(superLikeRepository);
    this.configurationService = Objects.requireNonNull(configurationService);
    if (freeLikeInterval <= 1) {
      throw new IllegalArgumentException("L'intervalle du like gratuit doit être strictement supérieur à 1.");
    }
    this.freeLikeInterval = freeLikeInterval;
  }

  @Override
  public SuperLikeImpact calculateImpact(UserEntity user, int quantity) {
    Objects.requireNonNull(user);
    ensureFreeLikePromotionType();

    var pastPurchases = superLikeRepository.countBySenderAndDateAfter(user, promo.getDateStart());
    var normalPrice = configurationService.getConfiguration().getSuperLikePrice();

    var totalToPay = BigDecimal.ZERO;
    var totalSiteShare = BigDecimal.ZERO;
    var totalDistributionShare = BigDecimal.ZERO;
    var freeCount = 0;

    for (var i = 1; i <= quantity; i++) {
      var isFreeLike = (pastPurchases + i) % freeLikeInterval == 0;
      if (isFreeLike) {
        freeCount++;
        var creatorShare = normalPrice.multiply(DISTRIBUTION_SHARE_RATIO);
        totalDistributionShare = totalDistributionShare.add(creatorShare);
        totalSiteShare = totalSiteShare.subtract(creatorShare);
      } else {
        totalToPay = totalToPay.add(normalPrice);
        totalSiteShare = totalSiteShare.add(normalPrice.multiply(SITE_SHARE_RATIO));
        totalDistributionShare = totalDistributionShare.add(normalPrice.multiply(DISTRIBUTION_SHARE_RATIO));
      }
    }

    var description = String.format("Offre %deme like gratuit : %d offert(s). Total : %s€",
        freeLikeInterval, freeCount, totalToPay.toPlainString());

    return new SuperLikeImpact(
        totalToPay,
        totalSiteShare,
        totalDistributionShare,
        freeCount,
        description,
        LocalDateTime.now(),
        promo
    );
  }

  private void ensureFreeLikePromotionType() {
    if (!FREE_LIKE_PROMOTION_TYPE.equalsIgnoreCase(promo.getType())) {
      throw new IllegalStateException("La promotion doit être de type FREE_LIKE pour utiliser cette stratégie.");
    }
  }
}