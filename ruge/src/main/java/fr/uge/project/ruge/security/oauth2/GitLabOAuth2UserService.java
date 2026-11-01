package fr.uge.project.ruge.security.oauth2;

import fr.uge.project.ruge.entity.WalletEntity;
import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.enums.Role;
import fr.uge.project.ruge.enums.SsoProvider;
import fr.uge.project.ruge.enums.Status;
import fr.uge.project.ruge.enums.WalletType;
import fr.uge.project.ruge.repositories.UserRepository;
import fr.uge.project.ruge.services.AccountLockService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.UUID;

@Service
public class GitLabOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
  private static final String GITLAB_REGISTRATION_ID = "gitlab";
  private static final String GITLAB_USER_ID_KEY = "id";
  private static final String GITLAB_EMAIL_KEY = "email";
  private static final String GITLAB_NAME_KEY = "name";
  private static final String GITLAB_USERNAME_KEY = "username";
  private static final String GITLAB_AVATAR_URL_KEY = "avatar_url";

  private final UserRepository userRepository;
  private final AccountLockService accountLockService;
  private final PasswordEncoder passwordEncoder;
  private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

  @Autowired
  public GitLabOAuth2UserService(UserRepository userRepository,
                                 AccountLockService accountLockService,
                                 PasswordEncoder passwordEncoder) {
    this(userRepository, accountLockService, passwordEncoder, new DefaultOAuth2UserService());
  }

  GitLabOAuth2UserService(UserRepository userRepository,
                          AccountLockService accountLockService,
                          PasswordEncoder passwordEncoder,
                          OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate) {
    this.userRepository = Objects.requireNonNull(userRepository);
    this.accountLockService = Objects.requireNonNull(accountLockService);
    this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
    this.delegate = Objects.requireNonNull(delegate);
  }

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    var oauth2User = delegate.loadUser(userRequest);
    if (!isGitLabRequest(userRequest)) {
      return oauth2User;
    }

    var profile = extractGitLabProfile(oauth2User);
    var user = synchronizeGitLabUser(profile);
    if (accountLockService.refreshAndIsLocked(user)) {
      throw authenticationError("banned", "User account is banned");
    }
    return buildAuthenticatedUser(oauth2User, user);
  }

  private static boolean isGitLabRequest(OAuth2UserRequest userRequest) {
    return GITLAB_REGISTRATION_ID.equals(userRequest.getClientRegistration().getRegistrationId());
  }

  private static GitLabProfile extractGitLabProfile(OAuth2User oauth2User) {
    var externalId = requiredAttribute(oauth2User, GITLAB_USER_ID_KEY);
    var email = requiredEmail(oauth2User);
    var name = firstNonBlank(
        asString(oauth2User.getAttributes().get(GITLAB_NAME_KEY)),
        asString(oauth2User.getAttributes().get(GITLAB_USERNAME_KEY)),
        email
    );
    var avatarUrl = asString(oauth2User.getAttributes().get(GITLAB_AVATAR_URL_KEY));
    return new GitLabProfile(externalId, email, name, avatarUrl);
  }

  private static DefaultOAuth2User buildAuthenticatedUser(OAuth2User oauth2User, UserEntity user) {
    var authorities = new LinkedHashSet<GrantedAuthority>(oauth2User.getAuthorities());
    var roleName = user.getRole() == null ? Role.MEMBER.name() : user.getRole().name();
    authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));

    var attributes = new HashMap<>(oauth2User.getAttributes());
    attributes.put(GITLAB_EMAIL_KEY, user.getEmail());
    attributes.put(GITLAB_NAME_KEY, user.getName());
    attributes.put(GITLAB_USER_ID_KEY, user.getSsoId());
    if (user.getImageUrl() != null) {
      attributes.put(GITLAB_AVATAR_URL_KEY, user.getImageUrl());
    }

    return new DefaultOAuth2User(authorities, attributes, GITLAB_EMAIL_KEY);
  }

  private UserEntity synchronizeGitLabUser(GitLabProfile profile) {
    return userRepository.findBySsoProviderAndSsoId(SsoProvider.GITLAB, profile.externalId())
        .map(user -> synchronizeKnownGitLabUser(user, profile))
        .orElseGet(() -> synchronizeByEmailOrCreate(profile));
  }

  private UserEntity synchronizeKnownGitLabUser(UserEntity user, GitLabProfile profile) {
    var changed = false;
    changed |= updateEmailIfPossible(user, profile.email());
    changed |= updateIfDifferent(user.getName(), profile.name(), user::setName);
    changed |= updateIfDifferent(user.getImageUrl(), profile.avatarUrl(), user::setImageUrl);
    changed |= ensureWallet(user);
    return changed ? userRepository.save(user) : user;
  }

  private UserEntity synchronizeByEmailOrCreate(GitLabProfile profile) {
    return userRepository.findByEmail(profile.email())
        .map(user -> linkExistingUserWithGitLab(user, profile))
        .orElseGet(() -> createNewGitLabUser(profile));
  }

  private UserEntity linkExistingUserWithGitLab(UserEntity user, GitLabProfile profile) {
    validateGitLabLink(user, profile.externalId());
    user.setSsoProvider(SsoProvider.GITLAB);
    user.setSsoId(profile.externalId());
    updateIfDifferent(user.getName(), profile.name(), user::setName);
    updateIfDifferent(user.getImageUrl(), profile.avatarUrl(), user::setImageUrl);
    ensureWallet(user);
    return userRepository.save(user);
  }

  private static void validateGitLabLink(UserEntity user, String externalId) {
    if (user.getSsoProvider() != null && user.getSsoProvider() != SsoProvider.GITLAB) {
      throw authenticationError("sso_conflict", "This email is already linked to another SSO provider");
    }
    if (user.getSsoId() != null && !externalId.equals(user.getSsoId())) {
      throw authenticationError("sso_conflict", "This account is linked to another GitLab profile");
    }
  }

  private UserEntity createNewGitLabUser(GitLabProfile profile) {
    var user = new UserEntity();
    user.setEmail(profile.email());
    user.setName(profile.name());
    user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
    user.setImageUrl(profile.avatarUrl());
    user.setRole(Role.MEMBER);
    user.setStatus(Status.ACTIVE);
    user.setSsoProvider(SsoProvider.GITLAB);
    user.setSsoId(profile.externalId());
    ensureWallet(user);
    return userRepository.save(user);
  }

  private static boolean ensureWallet(UserEntity user) {
    if (user.getWallet() != null) {
      return false;
    }
    var wallet = new WalletEntity();
    wallet.setMoney(BigDecimal.ZERO);
    wallet.setType(WalletType.USER.name());
    user.setWallet(wallet);
    return true;
  }

  private boolean updateEmailIfPossible(UserEntity user, String newEmail) {
    if (Objects.equals(user.getEmail(), newEmail)) {
      return false;
    }
    var existing = userRepository.findByEmail(newEmail);
    if (existing.isPresent() && !Objects.equals(existing.get().getId(), user.getId())) {
      throw authenticationError("email_conflict", "Email is already used by another account");
    }
    user.setEmail(newEmail);
    return true;
  }

  private static boolean updateIfDifferent(String current, String candidate, java.util.function.Consumer<String> setter) {
    if (Objects.equals(current, candidate)) {
      return false;
    }
    setter.accept(candidate);
    return true;
  }

  private static String requiredEmail(OAuth2User oauth2User) {
    var email = asString(oauth2User.getAttributes().get("email"));
    if (email == null || email.isBlank()) {
      throw authenticationError("missing_email", "Email is required to sign in with GitLab");
    }
    return email;
  }

  private static String requiredAttribute(OAuth2User oauth2User, String key) {
    var value = asString(oauth2User.getAttributes().get(key));
    if (value == null || value.isBlank()) {
      throw authenticationError("missing_attribute", "Required attribute is missing: " + key);
    }
    return value;
  }

  private static String asString(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private static String firstNonBlank(String first, String second, String third) {
    if (first != null && !first.isBlank()) {
      return first;
    }
    if (second != null && !second.isBlank()) {
      return second;
    }
    return third;
  }

  private static OAuth2AuthenticationException authenticationError(String code, String message) {
    return new OAuth2AuthenticationException(new OAuth2Error(code), message);
  }

  private record GitLabProfile(String externalId, String email, String name, String avatarUrl) {
  }
}

