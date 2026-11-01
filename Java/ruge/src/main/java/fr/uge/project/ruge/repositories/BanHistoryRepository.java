package fr.uge.project.ruge.repositories;

import fr.uge.project.ruge.entity.BanHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface BanHistoryRepository extends JpaRepository<BanHistoryEntity, Long> {

  Optional<BanHistoryEntity> findTopByBannedUserIdOrderByBanDateDesc(Long id);

  List<BanHistoryEntity> findByBannedUserId(Long id);
}

