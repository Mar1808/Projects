package fr.uge.project.ruge.repositories;

import fr.uge.project.ruge.entity.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<WalletEntity, Long> {
  Optional<WalletEntity> findFirstByType(String type);

  @Query("SELECT u.wallet FROM UserEntity u WHERE u.id = :userId")
  Optional<WalletEntity> findByUserId(@Param("userId") Long userId);
}

