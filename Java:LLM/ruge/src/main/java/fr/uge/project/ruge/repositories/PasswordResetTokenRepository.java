package fr.uge.project.ruge.repositories;

import fr.uge.project.ruge.entity.PasswordResetTokenEntity;
import fr.uge.project.ruge.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {
    PasswordResetTokenEntity findByToken(String token);
    void deleteByUser(UserEntity user);
}
