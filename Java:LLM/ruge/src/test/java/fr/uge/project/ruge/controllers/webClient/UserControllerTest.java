package fr.uge.project.ruge.controllers.webClient;

import fr.uge.project.ruge.dto.ProfilePhotoDto;
import fr.uge.project.ruge.dto.UserDto;
import fr.uge.project.ruge.enums.Role;
import fr.uge.project.ruge.services.PasswordResetService;
import fr.uge.project.ruge.services.UserService;
import fr.uge.project.ruge.testsupport.MockitoTestSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest extends MockitoTestSupport {
  @Mock
  private UserService userService;
  @Mock
  private AuthenticationManager authenticationManager;
  @Mock
  private PasswordResetService passwordResetService;
  @Mock
  private Authentication authentication;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void registerFormPopulatesModel() {
    UserController controller = controller();
    ExtendedModelMap model = new ExtendedModelMap();
    when(userService.getDefaultProfilePhotoId()).thenReturn(1L);
    when(userService.getAvailableProfilePhotos()).thenReturn(List.of(new ProfilePhotoDto(1L, "a1", "Avatar", "/a.png")));

    String view = controller.registerForm(model);

    assertThat(view).isEqualTo("auth/register");
    assertThat(model.getAttribute("user")).isNotNull();
    assertThat(model.getAttribute("profilePhotos")).isNotNull();
    assertThat(model.getAttribute("roles")).isNotNull();
  }

  @Test
  void registerUserReturnsRegisterViewWhenValidationFails() {
    UserController controller = controller();
    ExtendedModelMap model = new ExtendedModelMap();
    UserDto dto = new UserDto("user@ruge.test", "password123", "User", null, 1L, Role.MEMBER);
    BeanPropertyBindingResult binding = new BeanPropertyBindingResult(dto, "user");
    binding.rejectValue("email", "invalid", "invalid");
    when(userService.getAvailableProfilePhotos()).thenReturn(List.of());

    String view = controller.registerUser(
        dto,
        binding,
        model,
        new MockHttpServletRequest(),
        new MockHttpServletResponse()
    );

    assertThat(view).isEqualTo("auth/register");
  }

  @Test
  void registerUserAuthenticatesAndRedirectsHomeWhenSuccess() {
    UserController controller = controller();
    ExtendedModelMap model = new ExtendedModelMap();
    UserDto dto = new UserDto("user@ruge.test", "password123", "User", null, 1L, Role.MEMBER);
    BeanPropertyBindingResult binding = new BeanPropertyBindingResult(dto, "user");
    HttpServletRequest request = new MockHttpServletRequest();
    HttpServletResponse response = new MockHttpServletResponse();
    when(authenticationManager.authenticate(any())).thenReturn(authentication);

    String view = controller.registerUser(dto, binding, model, request, response);

    assertThat(view).isEqualTo("redirect:/home");
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
  }

  @Test
  void processForgotPasswordAddsMessage() {
    UserController controller = controller();
    ExtendedModelMap model = new ExtendedModelMap();

    String view = controller.processForgotPassword("user@ruge.test", model);

    assertThat(view).isEqualTo("auth/forgot-password");
    assertThat(model.getAttribute("message")).isEqualTo("If this email exists, a reset link has been sent.");
  }

  @Test
  void processResetPasswordRedirectsOnSuccess() {
    UserController controller = controller();
    ExtendedModelMap model = new ExtendedModelMap();
    when(passwordResetService.updatePassword("token", "newpass")).thenReturn("Success");

    String view = controller.processResetPassword("token", "newpass", model);

    assertThat(view).isEqualTo("redirect:/login?resetSuccess");
  }

  private UserController controller() {
    return new UserController(userService, authenticationManager, passwordResetService);
  }
}
