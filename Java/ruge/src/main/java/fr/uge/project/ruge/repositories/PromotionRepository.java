package fr.uge.project.ruge.repositories;

import fr.uge.project.ruge.entity.PromotionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRepository extends JpaRepository<PromotionEntity, Long> {
}