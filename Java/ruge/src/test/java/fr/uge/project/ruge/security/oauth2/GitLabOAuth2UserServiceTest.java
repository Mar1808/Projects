package fr.uge.project.ruge.security.oauth2;

import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.entity.WalletEntity;
import fr.uge.project.ruge.enums.Role;
import fr.uge.project.ruge.enums.SsoProvider;
import fr.uge.project.ruge.enums.Status;
import fr.uge.project.ruge.enums.WalletType;
import fr.uge.project.ruge.repositories.UserRepository;
import fr.uge.project.ruge.services.AccountLockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitLabOAuth2UserServiceTest {
  @Mock
  private UserRepository userRepository;
  @Mock
  private AccountLockService accountLockService;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

  @Test
  void loadUserReturnsDelegateUserWhenProviderIsNotGitLab() {
    var service = new GitLabOAuth2UserService(userRepository, accountLockService, passwordEncoder, delegate);
    var userRequest = userRequest("google");
    var delegateUser = oauth2User(Map.of("id", "42", "email", "person@example.com"));
    when(delegate.loadUser(userRequest)).thenReturn(delegateUser);

    var result = service.loadUser(userRequest);

    assertThat(result).isSameAs(delegateUser);
    verifyNoInteractions(userRepository, accountLockService, passwordEncoder);
  }

  @Test
  void loadUserCreatesGitLabUserWhenNotFound() {
    var service = new GitLabOAuth2UserService(userRepository, accountLockService, passwordEncoder, delegate);
    var userRequest = userRequest("gitlab");
    var oauthUser = oauth2User(Map.of(
        "id", "gitlab-123",
        "email", "new.user@ruge.test",
        "name", "New User",
        "avatar_url", "https://gitlab.example/avatar.png"
    ));

    when(delegate.loadUser(userRequest)).thenReturn(oauthUser);
    when(userRepository.findBySsoProviderAndSsoId(SsoProvider.GITLAB, "gitlab-123")).thenReturn(Optional.empty());
    when(userRepository.findByEmail("new.user@ruge.test")).thenReturn(Optional.empty());
    when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
    when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(accountLockService.refreshAndIsLocked(any(UserEntity.class))).thenReturn(false);

    var result = service.loadUser(userRequest);

    assertThat(result.<String>getAttribute("email")).isEqualTo("new.user@ruge.test");
    assertThat(result.<String>getAttribute("name")).isEqualTo("New User");
    assertThat(result.<String>getAttribute("id")).isEqualTo("gitlab-123");
    assertThat(result.<String>getAttribute("avatar_url")).isEqualTo("https://gitlab.example/avatar.png");
    assertThat(result.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .contains("ROLE_MEMBER");
  }

  @Test
  void loadUserThrowsWhenAccountIsLocked() {
    var service = new GitLabOAuth2UserService(userRepository, accountLockService, passwordEncoder, delegate);
    var userRequest = userRequest("gitlab");
    var oauthUser = oauth2User(Map.of(
        "id", "gitlab-123",
        "email", "member@ruge.test",
        "name", "Member"
    ));
    var existingUser = existingGitLabUser("gitlab-123", "member@ruge.test");

    when(delegate.loadUser(userRequest)).thenReturn(oauthUser);
    when(userRepository.findBySsoProviderAndSsoId(SsoProvider.GITLAB, "gitlab-123")).thenReturn(Optional.of(existingUser));
    when(accountLockService.refreshAndIsLocked(existingUser)).thenReturn(true);

    assertThatThrownBy(() -> service.loadUser(userRequest))
        .isInstanceOf(OAuth2AuthenticationException.class)
        .extracting("error.errorCode")
        .isEqualTo("banned");
  }

  @Test
  void loadUserThrowsOnGitLabProfileConflictForSameEmail() {
    var service = new GitLabOAuth2UserService(userRepository, accountLockService, passwordEncoder, delegate);
    var userRequest = userRequest("gitlab");
    var oauthUser = oauth2User(Map.of(
        "id", "gitlab-new-id",
        "email", "member@ruge.test",
        "name", "Member"
    ));
    var sameEmailUser = existingGitLabUser("gitlab-old-id", "member@ruge.test");

    when(delegate.loadUser(userRequest)).thenReturn(oauthUser);
    when(userRepository.findBySsoProviderAndSsoId(SsoProvider.GITLAB, "gitlab-new-id")).thenReturn(Optional.empty());
    when(userRepository.findByEmail("member@ruge.test")).thenReturn(Optional.of(sameEmailUser));

    assertThatThrownBy(() -> service.loadUser(userRequest))
        .isInstanceOf(OAuth2AuthenticationException.class)
        .extracting("error.errorCode")
        .isEqualTo("sso_conflict");
  }

  private static OAuth2UserRequest userRequest(String registrationId) {
    var request = mock(OAuth2UserRequest.class);
    var registration = mock(ClientRegistration.class);
    when(request.getClientRegistration()).thenReturn(registration);
    when(registration.getRegistrationId()).thenReturn(registrationId);
    return request;
  }

  private static OAuth2User oauth2User(Map<String, Object> attributes) {
    return new DefaultOAuth2User(
        Set.of(new SimpleGrantedAuthority("ROLE_OAUTH_USER")),
        new HashMap<>(attributes),
        "email"
    );
  }

  private static UserEntity existingGitLabUser(String externalId, String email) {
    var user = new UserEntity();
    user.setId(12L);
    user.setEmail(email);
    user.setName("Member");
    user.setPassword("encoded");
    user.setRole(Role.MEMBER);
    user.setStatus(Status.ACTIVE);
    user.setSsoProvider(SsoProvider.GITLAB);
    user.setSsoId(externalId);
    var wallet = new WalletEntity();
    wallet.setType(WalletType.USER.name());
    wallet.setMoney(BigDecimal.ZERO);
    user.setWallet(wallet);
    return user;
  }
}
