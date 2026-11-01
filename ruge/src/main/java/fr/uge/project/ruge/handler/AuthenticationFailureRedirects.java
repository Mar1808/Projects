package fr.uge.project.ruge.handler;

import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

final class AuthenticationFailureRedirects {
  private static final String LOGIN_ERROR_URL = "/login?error";
  private static final String LOGIN_BANNED_URL = "/login?banned";

  private AuthenticationFailureRedirects() {
  }

  static String resolveFormFailureUrl(AuthenticationException exception) {
    return switch (exception) {
      case LockedException ignored -> LOGIN_BANNED_URL;
      default -> LOGIN_ERROR_URL;
    };
  }

  static String resolveOauth2FailureUrl(AuthenticationException exception) {
    return switch (exception) {
      case OAuth2AuthenticationException oae when "banned".equals(oae.getError().getErrorCode()) -> LOGIN_BANNED_URL;
      default -> LOGIN_ERROR_URL;
    };
  }
}
