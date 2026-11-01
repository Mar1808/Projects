package fr.uge.project.ruge.controllers.advice;

import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.exceptions.UserNotFoundException;
import fr.uge.project.ruge.helper.CurrentUserSupport;
import fr.uge.project.ruge.services.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.math.BigDecimal;
import java.util.Objects;

@ControllerAdvice
public class AuthStateAdvice {
  private final UserService userService;

  public AuthStateAdvice(UserService userService) {
    this.userService = Objects.requireNonNull(userService);
  }

  @ModelAttribute("isAuthenticated")
  public boolean isAuthenticated(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || "anonymousUser".equals(authentication.getName())) {
      return false;
    }
    return userService.findByEmail(authentication.getName()).isPresent();
  }

  @ModelAttribute("isAdmin")
  public boolean isAdmin(Authentication authentication) {
    if (!isAuthenticated(authentication)) {
      return false;
    }
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch("ROLE_ADMIN"::equals);
  }

  @ModelAttribute("currentUserId")
  public Long currentUserId(Authentication authentication) {
    return CurrentUserSupport.currentUser(userService, authentication)
        .map(UserEntity::getId)
        .orElse(null);
  }

  @ModelAttribute("userBalance")
  public BigDecimal userBalance(Authentication authentication) {
    if (!isAuthenticated(authentication)) {
      return null;
    }
    try {
      return userService.getWalletBalance(authentication.getName());
    } catch (UserNotFoundException | IllegalStateException e) {
      return null;
    }
  }

  @ModelAttribute("currentUserName")
  public String currentUserName(Authentication authentication) {
    return CurrentUserSupport.currentUser(userService, authentication)
        .map(UserEntity::getName)
        .orElse(null);
  }

  @ModelAttribute("image")
  public String image(Authentication authentication) {
    return CurrentUserSupport.currentUser(userService, authentication)
        .map(UserEntity::getImageUrl)
        .orElse(null);
  }
}
