package fr.uge.project.ruge.controllers.advice;

import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.enums.Role;
import fr.uge.project.ruge.enums.Status;
import fr.uge.project.ruge.exceptions.UserNotFoundException;
import fr.uge.project.ruge.services.UserService;
import fr.uge.project.ruge.testsupport.MockitoTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthStateAdviceTest extends MockitoTestSupport {
  @Mock
  private UserService userService;

  @Test
  void isAuthenticatedReturnsFalseForAnonymous() {
    AuthStateAdvice advice = new AuthStateAdvice(userService);
    TestingAuthenticationToken auth = new TestingAuthenticationToken("anonymousUser", "pwd");

    assertThat(advice.isAuthenticated(auth)).isFalse();
  }

  @Test
  void isAuthenticatedReturnsTrueWhenUserExists() {
    AuthStateAdvice advice = new AuthStateAdvice(userService);
    TestingAuthenticationToken auth = new TestingAuthenticationToken("user@ruge.test", "pwd", "ROLE_MEMBER");
    when(userService.findByEmail("user@ruge.test")).thenReturn(Optional.of(user(1L, "user@ruge.test", Role.MEMBER)));

    assertThat(advice.isAuthenticated(auth)).isTrue();
  }

  @Test
  void isAdminReturnsTrueForAdminAuthority() {
    AuthStateAdvice advice = new AuthStateAdvice(userService);
    TestingAuthenticationToken auth = new TestingAuthenticationToken("admin@ruge.test", "pwd", "ROLE_ADMIN");
    when(userService.findByEmail("admin@ruge.test")).thenReturn(Optional.of(user(2L, "admin@ruge.test", Role.ADMIN)));

    assertThat(advice.isAdmin(auth)).isTrue();
  }

  @Test
  void userBalanceReturnsNullWhenLookupFails() {
    AuthStateAdvice advice = new AuthStateAdvice(userService);
    TestingAuthenticationToken auth = new TestingAuthenticationToken("user@ruge.test", "pwd", "ROLE_MEMBER");
    when(userService.findByEmail("user@ruge.test")).thenReturn(Optional.of(user(1L, "user@ruge.test", Role.MEMBER)));
    when(userService.getWalletBalance("user@ruge.test")).thenThrow(new UserNotFoundException("missing"));

    assertThat(advice.userBalance(auth)).isNull();
  }

  @Test
  void currentUserNameReturnsNameWhenAuthenticated() {
    AuthStateAdvice advice = new AuthStateAdvice(userService);
    TestingAuthenticationToken auth = new TestingAuthenticationToken("user@ruge.test", "pwd", "ROLE_MEMBER");
    UserEntity user = user(1L, "user@ruge.test", Role.MEMBER);
    user.setName("Ruge User");
    when(userService.findByEmail("user@ruge.test")).thenReturn(Optional.of(user));

    assertThat(advice.currentUserName(auth)).isEqualTo("Ruge User");
  }

  private static UserEntity user(Long id, String email, Role role) {
    UserEntity user = new UserEntity();
    user.setId(id);
    user.setEmail(email);
    user.setRole(role);
    user.setPassword("pwd");
    user.setStatus(Status.ACTIVE);
    return user;
  }
}
