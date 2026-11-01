package fr.uge.project.ruge.services.observer;

import fr.uge.project.ruge.entity.PromotionEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PromoObserverUpdated implements PromoObserver {
    private final SimpMessagingTemplate messagingTemplate;

    public PromoObserverUpdated(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onPromoAdded(PromotionEntity promo) {

    }

    @Override
    public void onPromoUpdated(PromotionEntity promo) {
        // Logique pour gérer la mise à jour d'une promotion
        // Par exemple, envoyer une notification aux utilisateurs abonnés
        log.info("Promotion mise à jour : {} à {} du {} au {}", promo.getType(), promo.getPromoPrice(), promo.getDateStart(), promo.getDateEnd());
        messagingTemplate.convertAndSend("/topic/promotions", "Promotion mise à jour : " + promo.getType() + " à " + promo.getPromoPrice() + " du " + promo.getDateStart() + " au " + promo.getDateEnd());
    }
}
