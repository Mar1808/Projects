package fr.uge.project.ruge.services.observer;

import fr.uge.project.ruge.entity.PromotionEntity;

public interface PromoObserver {
    void onPromoAdded(PromotionEntity promo);
    void onPromoUpdated(PromotionEntity promo);
}
