package fr.uge.project.ruge.repositories;

import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.enums.Role;
import fr.uge.project.ruge.enums.SsoProvider;
import fr.uge.project.ruge.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
  Optional<UserEntity> findByEmail(String email);
  Optional<UserEntity> findBySsoProviderAndSsoId(SsoProvider ssoProvider, String ssoId);
  long countByStatus(Status status);
  long countByRole(Role role);
}
