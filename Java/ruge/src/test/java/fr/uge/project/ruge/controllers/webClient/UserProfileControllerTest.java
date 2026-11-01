package fr.uge.project.ruge.controllers.webClient;

import fr.uge.project.ruge.dto.SimpleRecipeDto;
import fr.uge.project.ruge.dto.UserLikeReactionDto;
import fr.uge.project.ruge.dto.UserReviewReactionDto;
import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.enums.Role;
import fr.uge.project.ruge.enums.Status;
import fr.uge.project.ruge.exceptions.UserNotFoundException;
import fr.uge.project.ruge.services.FollowService;
import fr.uge.project.ruge.services.LikerService;
import fr.uge.project.ruge.services.RecipeReviewService;
import fr.uge.project.ruge.services.RecipeService;
import fr.uge.project.ruge.services.UserService;
import fr.uge.project.ruge.testsupport.MockitoTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest extends MockitoTestSupport {
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

  @Test
  void viewProfileThrowsNotFoundWhenUserIsMissing() {
    UserProfileController controller = controller();
    when(userService.getByIdOrThrow(404L)).thenThrow(new UserNotFoundException("missing"));

    assertThatThrownBy(() -> controller.viewProfile(404L, new ExtendedModelMap(), null))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void viewProfilePopulatesModelForVisitor() {
    UserProfileController controller = controller();
    ExtendedModelMap model = new ExtendedModelMap();
    UserEntity profileUser = user(10L, "profile@ruge.test");
    UserEntity currentUser = user(20L, "visitor@ruge.test");
    var recipes = List.of(new SimpleRecipeDto(1L, "Cake", null, 30, "Profile"));
    var reviews = List.of(new UserReviewReactionDto(1L, "Cake", "Great", LocalDateTime.now()));
    var likes = List.of(new UserLikeReactionDto(1L, "Cake", LocalDateTime.now()));
    var auth = new TestingAuthenticationToken("visitor@ruge.test", "pwd", "ROLE_MEMBER");

    when(userService.getByIdOrThrow(10L)).thenReturn(profileUser);
    when(recipeService.getRecipesByCreatorId(10L)).thenReturn(recipes);
    when(recipeReviewService.getReviewsByAuthorId(10L)).thenReturn(reviews);
    when(likerService.getLikesBySenderId(10L)).thenReturn(likes);
    when(userService.findByEmail("visitor@ruge.test")).thenReturn(Optional.of(currentUser));
    when(followService.isFollowing(20L, 10L)).thenReturn(true);

    String view = controller.viewProfile(10L, model, auth);

    assertThat(view).isEqualTo("users/profile");
    assertThat(model.getAttribute("isSelf")).isEqualTo(false);
    assertThat(model.getAttribute("isFollowing")).isEqualTo(true);
    assertThat(model.getAttribute("recipeCount")).isEqualTo(1);
  }

  @Test
  void followRedirectsToLoginWhenNoAuthenticatedUser() {
    UserProfileController controller = controller();
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

    String view = controller.follow(12L, redirect, null);

    assertThat(view).isEqualTo("redirect:/login");
  }

  @Test
  void updateProfilePhotoRejectsUpdateForAnotherUser() {
    UserProfileController controller = controller();
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    UserEntity currentUser = user(2L, "user@ruge.test");
    var auth = new TestingAuthenticationToken("user@ruge.test", "pwd", "ROLE_MEMBER");
    when(userService.findByEmail("user@ruge.test")).thenReturn(Optional.of(currentUser));

    String view = controller.updateProfilePhoto(1L, 99L, redirect, auth);

    assertThat(view).isEqualTo("redirect:/users/1");
    assertThat(redirect.getFlashAttributes().get("errorMessage")).isEqualTo("Unauthorized action.");
  }

  private UserProfileController controller() {
    return new UserProfileController(recipeService, recipeReviewService, likerService, followService, userService);
  }

  private static UserEntity user(Long id, String email) {
    UserEntity user = new UserEntity();
    user.setId(id);
    user.setEmail(email);
    user.setName("User " + id);
    user.setPassword("pwd");
    user.setRole(Role.MEMBER);
    user.setStatus(Status.ACTIVE);
    return user;
  }
}
