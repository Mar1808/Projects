package fr.uge.project.ruge.repositories;

import fr.uge.project.ruge.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    List<NotificationEntity> findTop1ByOrderByCreatedAtDesc();
}