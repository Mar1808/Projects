package fr.uge.project.ruge.services.observer;

import fr.uge.project.ruge.entity.PromotionEntity;
import fr.uge.project.ruge.entity.NotificationEntity;
import fr.uge.project.ruge.repositories.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PromoObserverAdded implements PromoObserver {
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;

    public PromoObserverAdded(SimpMessagingTemplate messagingTemplate, NotificationRepository notificationRepository) {
        this.messagingTemplate = messagingTemplate;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void onPromoAdded(PromotionEntity promo) {
        handlePromotion(promo, "Nouvelle Promotion !");
    }

    @Override
    public void onPromoUpdated(PromotionEntity promo) {
        handlePromotion(promo, "Promotion Activée !");
    }

    // Méthode commune pour éviter de répéter le code
    private void handlePromotion(PromotionEntity promo, String title) {
        String message = "Profitez de " + promo.getType() + " à " + promo.getPromoPrice() + "€";

        log.info("Vérification de la promotion... Est-elle active ? : {}", promo.isActive());

        notificationRepository.save(new NotificationEntity(title, message));


        // On prévient toujours le client web pour qu'il rafraîchisse son affichage
        messagingTemplate.convertAndSend("/topic/promotions", title + ": " + message);
    }
}