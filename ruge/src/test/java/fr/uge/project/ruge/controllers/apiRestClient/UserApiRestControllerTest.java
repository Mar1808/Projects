package fr.uge.project.ruge.controllers.apiRestClient;

import fr.uge.project.ruge.dto.LoginDto;
import fr.uge.project.ruge.dto.ProfilePhotoDto;
import fr.uge.project.ruge.dto.UserDto;
import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.enums.Role;
import fr.uge.project.ruge.exceptions.UserAlreadyExistsException;
import fr.uge.project.ruge.services.UserService;
import fr.uge.project.ruge.services.rest.JwtService;
import fr.uge.project.ruge.testsupport.MockitoTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserApiRestControllerTest extends MockitoTestSupport {
  @Mock
  private UserService userService;
  @Mock
  private AuthenticationManager authenticationManager;
  @Mock
  private JwtService jwtService;
  @Mock
  private Authentication authentication;

  @Test
  void registerReturnsOkWhenUserIsCreated() {
    UserApiRestController controller = controller();
    UserDto dto = new UserDto("user@ruge.test", "password123", "User", null, null, Role.MEMBER);

    var response = controller.register(dto);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("Account created successfully");
  }

  @Test
  void registerReturnsBadRequestWhenUserAlreadyExists() {
    UserApiRestController controller = controller();
    UserDto dto = new UserDto("user@ruge.test", "password123", "User", null, null, Role.MEMBER);
    doThrow(new UserAlreadyExistsException("already exists")).when(userService).registerUser(dto);

    var response = controller.register(dto);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isEqualTo("already exists");
  }

  @Test
  void getProfilePhotosReturnsServicePayload() {
    UserApiRestController controller = controller();
    var photos = List.of(new ProfilePhotoDto(1L, "a1", "Avatar", "/img/a1.png"));
    when(userService.getAvailableProfilePhotos()).thenReturn(photos);

    var response = controller.getProfilePhotos();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(photos);
  }

  @Test
  void loginReturnsJwtToken() {
    UserApiRestController controller = controller();
    var user = new UserEntity();
    user.setId(8L);
    user.setEmail("user@ruge.test");
    user.setName("User");
    user.setRole(Role.MEMBER);

    when(userService.findByEmail("user@ruge.test")).thenReturn(Optional.of(user));
    when(authenticationManager.authenticate(any())).thenReturn(authentication);
    when(jwtService.generateToken(authentication)).thenReturn("jwt-token");

    var response = controller.login(new LoginDto("user@ruge.test", "password123"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().token()).isEqualTo("jwt-token");
  }

  private UserApiRestController controller() {
    return new UserApiRestController(userService, authenticationManager, jwtService);
  }
}
