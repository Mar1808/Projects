package fr.uge.project.ruge.config;

import fr.uge.project.ruge.handler.CustomAuthFailureHandler;
import fr.uge.project.ruge.handler.CustomOauth2AuthFailureHandler;
import fr.uge.project.ruge.repositories.UserRepository;
import fr.uge.project.ruge.security.oauth2.GitLabOAuth2UserService;
import fr.uge.project.ruge.services.AccountLockService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                 CustomAuthFailureHandler customAuthFailureHandler,
                                                 CustomOauth2AuthFailureHandler customOauth2AuthFailureHandler,
                                                 GitLabOAuth2UserService gitLabOAuth2UserService) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests((requests) -> requests
            .requestMatchers("/", "/home", "/register", "/login", "/oauth2/**", "/login/oauth2/**", "/recipes/**", "/terms", "/images/**", "/ws").permitAll()
            .requestMatchers(HttpMethod.GET, "/users/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/stripe/webhook").permitAll()
            .requestMatchers("/api/**").permitAll()
            .requestMatchers("/reset-password/**").permitAll()
            .requestMatchers("/forgot-password/**").permitAll()
            .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/notifications/**").authenticated()
            .anyRequest().authenticated()
        )
        .exceptionHandling((exceptions) -> exceptions.
            accessDeniedPage("/home")
        )
        .formLogin((form) -> form
            .loginPage("/login")
            .usernameParameter("email")
            .defaultSuccessUrl("/home", true)
            .failureHandler(customAuthFailureHandler)
            .permitAll()
        )
        .oauth2Login((oauth2) -> oauth2
            .loginPage("/login")
            .defaultSuccessUrl("/home", true)
            .userInfoEndpoint((userInfo) -> userInfo.userService(gitLabOAuth2UserService))
            .failureHandler(customOauth2AuthFailureHandler)
        )
        .logout((logout) -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/login?logout")
            .invalidateHttpSession(true)
            .clearAuthentication(true)
            .permitAll()
        );

    return http.build();
  }

  @Bean
  public UserDetailsService userDetailsService(UserRepository userRepository,
                                               AccountLockService accountLockService) {
    return (email) -> {
      var user = userRepository.findByEmail(email)
          .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
      var roleName = user.getRole() == null ? "MEMBER" : user.getRole().name();
      var isLocked = accountLockService.refreshAndIsLocked(user);

      return User.builder()
          .username(user.getEmail())
          .password(user.getPassword())
          .authorities("ROLE_" + roleName)
          .accountLocked(isLocked)
          .build();
    };
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
    return configuration.getAuthenticationManager();
  }
}
