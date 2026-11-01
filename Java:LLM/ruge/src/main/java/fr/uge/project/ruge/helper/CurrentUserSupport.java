package fr.uge.project.ruge.helper;

import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.services.UserService;
import org.springframework.security.core.Authentication;

import java.util.Optional;

public final class CurrentUserSupport {
  private CurrentUserSupport() {}

  public static Optional<UserEntity> currentUser(UserService userService, Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return Optional.empty();
    }

    var principalName = authentication.getName();
    if (principalName == null || principalName.isBlank() || "anonymousUser".equals(principalName)) {
      return Optional.empty();
    }
    return userService.findByEmail(principalName);
  }
}
