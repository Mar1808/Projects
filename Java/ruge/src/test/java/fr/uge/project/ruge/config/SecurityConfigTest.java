package fr.uge.project.ruge.config;

import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.enums.Role;
import fr.uge.project.ruge.enums.Status;
import fr.uge.project.ruge.handler.CustomAuthFailureHandler;
import fr.uge.project.ruge.handler.CustomOauth2AuthFailureHandler;
import fr.uge.project.ruge.repositories.UserRepository;
import fr.uge.project.ruge.security.oauth2.GitLabOAuth2UserService;
import fr.uge.project.ruge.services.AccountLockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {
  static {
    System.setProperty("net.bytebuddy.experimental", "true");
  }

  @Mock
  private UserRepository userRepository;

  @Mock
  private AuthenticationConfiguration authenticationConfiguration;

  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private HttpSecurity httpSecurity;

  @Mock
  private CustomAuthFailureHandler customAuthFailureHandler;

  @Mock
  private CustomOauth2AuthFailureHandler customOauth2AuthFailureHandler;

  @Mock
  private GitLabOAuth2UserService gitLabOAuth2UserService;

  @Mock
  private AccountLockService accountLockService;

  @Mock
  private DefaultSecurityFilterChain builtSecurityFilterChain;

  private final SecurityConfig securityConfig = new SecurityConfig();

  @Test
  void userDetailsServiceThrowsWhenUserDoesNotExist() {
    UserDetailsService userDetailsService = securityConfig.userDetailsService(userRepository, accountLockService);
    when(userRepository.findByEmail("unknown@ruge.test")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown@ruge.test"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessage("User not found: unknown@ruge.test");
  }

  @Test
  void userDetailsServiceReturnsUnlockedUserWithRole() {
    UserEntity user = user(1L, "admin@ruge.test", Role.ADMIN, Status.ACTIVE);
    UserDetailsService userDetailsService = securityConfig.userDetailsService(userRepository, accountLockService);
    when(userRepository.findByEmail("admin@ruge.test")).thenReturn(Optional.of(user));
    when(accountLockService.refreshAndIsLocked(user)).thenReturn(false);

    UserDetails userDetails = userDetailsService.loadUserByUsername("admin@ruge.test");

    assertThat(userDetails.getUsername()).isEqualTo("admin@ruge.test");
    assertThat(userDetails.isAccountNonLocked()).isTrue();
    assertThat(userDetails.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_ADMIN");
    verify(accountLockService).refreshAndIsLocked(user);
  }

  @Test
  void userDetailsServiceUsesMemberRoleWhenRoleIsNull() {
    UserEntity user = user(2L, "member@ruge.test", null, Status.ACTIVE);
    UserDetailsService userDetailsService = securityConfig.userDetailsService(userRepository, accountLockService);
    when(userRepository.findByEmail("member@ruge.test")).thenReturn(Optional.of(user));
    when(accountLockService.refreshAndIsLocked(user)).thenReturn(false);

    UserDetails userDetails = userDetailsService.loadUserByUsername("member@ruge.test");

    assertThat(userDetails.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_MEMBER");
  }

  @Test
  void userDetailsServiceReturnsLockedUserWhenAccountIsLocked() {
    UserEntity user = user(3L, "banned@ruge.test", Role.MEMBER, Status.BANNED);
    UserDetailsService userDetailsService = securityConfig.userDetailsService(userRepository, accountLockService);

    when(userRepository.findByEmail("banned@ruge.test")).thenReturn(Optional.of(user));
    when(accountLockService.refreshAndIsLocked(user)).thenReturn(true);

    UserDetails userDetails = userDetailsService.loadUserByUsername("banned@ruge.test");

    assertThat(userDetails.isAccountNonLocked()).isFalse();
    verify(accountLockService).refreshAndIsLocked(user);
  }

  @Test
  void passwordEncoderIsBCryptAndMatchesRawPassword() {
    PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

    assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
    assertThat(passwordEncoder.matches("secret", passwordEncoder.encode("secret"))).isTrue();
  }

  @Test
  void authenticationManagerComesFromAuthenticationConfiguration() throws Exception {
    when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

    AuthenticationManager result = securityConfig.authenticationManager(authenticationConfiguration);

    assertThat(result).isSameAs(authenticationManager);
    verify(authenticationConfiguration).getAuthenticationManager();
  }

  private static UserEntity user(Long id, String email, Role role, Status status) {
    UserEntity user = new UserEntity();
    user.setId(id);
    user.setEmail(email);
    user.setPassword("encoded-password");
    user.setRole(role);
    user.setStatus(status);
    return user;
  }

}
