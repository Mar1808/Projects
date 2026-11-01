package fr.uge.project.ruge.services;

import fr.uge.project.ruge.dto.Promotion;
import fr.uge.project.ruge.entity.PromotionEntity;
import fr.uge.project.ruge.repositories.PromotionRepository;
import fr.uge.project.ruge.services.observer.PromoObserver;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final List<PromoObserver> promoObservers;

    public PromotionService(PromotionRepository promotionRepository, List<PromoObserver> promoObservers) {
        this.promotionRepository = promotionRepository;
        this.promoObservers = promoObservers;
    }

    public List<Promotion> getAllPromotions() {
        return promotionRepository.findAll().stream()
                .map(entity -> new Promotion(
                        entity.getId(), entity.getType(), entity.getPromoPrice(),
                        entity.getDateStart(), entity.getDateEnd()
                )).toList();
    }

    public void createPromotion(String type, BigDecimal price, LocalDateTime start, LocalDateTime end) {
        if (hasOverlappingPromotion(start, end, null)) {
            throw new IllegalArgumentException("Une autre promotion est déjà prévue sur cette période.");
        }

        PromotionEntity entity = new PromotionEntity();
        entity.setType(type);
        entity.setPromoPrice(price);
        entity.setDateStart(start);
        entity.setDateEnd(end);
        promotionRepository.save(entity);
        promoObservers.forEach(obs -> obs.onPromoAdded(entity));
    }

    public void deletePromotion(Long id) {
        PromotionEntity promo = promotionRepository.findById(id).orElseThrow();
        if (promo.isActive()) {
            throw new IllegalStateException("Impossible de supprimer une promotion en cours.");
        }
        promotionRepository.deleteById(id);
    }

    private boolean hasOverlappingPromotion(LocalDateTime start, LocalDateTime end, Long excludeId) {
        return promotionRepository.findAll().stream()
                .filter(p -> excludeId == null || !p.getId().equals(excludeId))
                .anyMatch(p -> start.isBefore(p.getDateEnd()) && end.isAfter(p.getDateStart()));
    }

    public Promotion getPromotionById(Long id) {
        return promotionRepository.findById(id)
                .map(entity -> new Promotion(
                        entity.getId(), entity.getType(), entity.getPromoPrice(),
                        entity.getDateStart(), entity.getDateEnd()
                )).orElseThrow(() -> new IllegalArgumentException("Promotion introuvable"));
    }
    @Transactional
    public void updatePromotion(Long id, BigDecimal price, LocalDateTime start, LocalDateTime end) {
        PromotionEntity entity = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion introuvable"));

        start = start != null ? start : entity.getDateStart();
        end = end != null ? end : entity.getDateEnd();
        price  = price != null ? price : entity.getPromoPrice();

        LocalDateTime now = LocalDateTime.now();

        // Impossible de définir une date dans le passé
        if (start.isBefore(now) || end.isBefore(now)) {
            throw new IllegalArgumentException("Les dates de promotion ne peuvent pas être dans le passé.");
        }

        // Non modifiable si passée
        if (entity.getDateStart().isBefore(now) && entity.getDateEnd().isBefore(now)) {
            throw new IllegalStateException("Impossible de modifier une promotion déjà passée.");
        }

        // Pas de chevauchement
        if (hasOverlappingPromotion(start, end, id)) {
            throw new IllegalArgumentException("Une autre promotion est déjà prévue sur cette période.");
        }

        // La date et le prix ne peuvent être modifiés si la promotion est en cours
        if (entity.isActive() && (!entity.getDateStart().isEqual(start)) || !entity.getPromoPrice().equals(price)) {
            throw new IllegalStateException("Impossible de modifier le prix ou les dates d'une promotion en cours.");
        }

        entity.setPromoPrice(price);
        entity.setDateStart(start);
        entity.setDateEnd(end);

        promotionRepository.save(entity);
        promoObservers.forEach(obs -> obs.onPromoUpdated(entity));
    }
}