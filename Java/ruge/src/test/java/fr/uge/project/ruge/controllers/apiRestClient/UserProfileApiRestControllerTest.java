package fr.uge.project.ruge.controllers.apiRestClient;

import fr.uge.project.ruge.dto.SimpleRecipeDto;
import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.enums.Role;
import fr.uge.project.ruge.enums.Status;
import fr.uge.project.ruge.services.FollowService;
import fr.uge.project.ruge.services.LikerService;
import fr.uge.project.ruge.services.RecipeReviewService;
import fr.uge.project.ruge.services.RecipeService;
import fr.uge.project.ruge.services.UserService;
import fr.uge.project.ruge.services.rest.JwtService;
import fr.uge.project.ruge.testsupport.MockitoTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileApiRestControllerTest extends MockitoTestSupport {
  @Mock
  private RecipeService recipeService;
  @Mock
  private RecipeReviewService recipeReviewService;
  @Mock
  private LikerService likerService;
  @Mock
  private FollowService followService;
  @Mock
  private UserService userService;
  @Mock
  private JwtService jwtService;

  @Test
  void getMyRecipesReturnsUnauthorizedWhenTokenInvalid() {
    var controller = controller();
    when(jwtService.validateToken("bad")).thenReturn(false);

    var response = controller.getMyRecipes("bad");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void getMyRecipesReturnsRecipesForAuthenticatedUser() {
    var controller = controller();
    var user = user(4L, "user@ruge.test");
    var recipes = List.of(new SimpleRecipeDto(12L, "Soup", null, 20, "User"));
    when(jwtService.validateToken("token")).thenReturn(true);
    when(jwtService.extractUsername("token")).thenReturn("user@ruge.test");
    when(userService.findByEmail("user@ruge.test")).thenReturn(Optional.of(user));
    when(recipeService.getRecipesByCreatorId(4L)).thenReturn(recipes);

    var response = controller.getMyRecipes("token");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(recipes);
  }

  @Test
  void updateMyProfilePhotoReturnsBadRequestWhenServiceRejectsInput() {
    var controller = controller();
    when(jwtService.validateToken("token")).thenReturn(true);
    when(jwtService.extractUsername("token")).thenReturn("user@ruge.test");
    doThrow(new IllegalArgumentException("Invalid profile photo."))
        .when(userService)
        .updateProfilePhotoByEmail("user@ruge.test", 99L);

    var response = controller.updateMyProfilePhoto("token", 99L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isEqualTo("Invalid profile photo.");
  }

  private UserProfileApiRestController controller() {
    return new UserProfileApiRestController(
        recipeService,
        userService,
        jwtService,
        recipeReviewService,
        likerService
    );
  }

  private static UserEntity user(Long id, String email) {
    UserEntity user = new UserEntity();
    user.setId(id);
    user.setEmail(email);
    user.setPassword("pwd");
    user.setRole(Role.MEMBER);
    user.setStatus(Status.ACTIVE);
    return user;
  }
}
