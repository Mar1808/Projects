package fr.uge.project.ruge.controllers.webClient;

import fr.uge.project.ruge.dto.RecipeCreateDto;
import fr.uge.project.ruge.dto.SimpleRecipeDto;
import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.enums.Role;
import fr.uge.project.ruge.enums.Status;
import fr.uge.project.ruge.exceptions.RecipeNotFoundException;
import fr.uge.project.ruge.factory.SuperLikeStrategyFactory;
import fr.uge.project.ruge.services.*;
import fr.uge.project.ruge.testsupport.MockitoTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeControllerTest extends MockitoTestSupport {
  @Mock
  private RecipeService recipeService;
  @Mock
  private RecipeReviewService recipeReviewService;
  @Mock
  private FollowRecipeService followRecipeService;
  @Mock
  private LikerService likerService;
  @Mock
  private SuperLikeService superLikeService;
  @Mock
  private UserService userService;
  @Mock
  private RecipeAiService recipeAiService;
  @Mock
  private ConfigurationService configurationService;
  @Mock
  private WalletService walletService;
  @Mock
  private SuperLikeStrategyFactory superLikeStrategyFactory;

  @Test
  void listRecipesRedirectsToLoginWhenPrincipalIsMissing() {
    RecipeController controller = controller();

    String view = controller.listRecipes(0, new ExtendedModelMap(), null);

    assertThat(view).isEqualTo("redirect:/login");
  }

  @Test
  void listRecipesPopulatesModelForAuthenticatedUser() {
    RecipeController controller = controller();
    ExtendedModelMap model = new ExtendedModelMap();
    Principal principal = () -> "user@ruge.test";
    UserEntity user = user(3L, "user@ruge.test");
    var page = new PageImpl<>(List.of(new SimpleRecipeDto(10L, "Soup", null, 20, "User")));

    when(userService.findByEmail("user@ruge.test")).thenReturn(Optional.of(user));
    when(recipeService.getRecipesByCreatorIdPageable(3L, 0)).thenReturn(page);
    when(walletService.getAiSearchPrice(user)).thenReturn(new BigDecimal("0.80"));

    String view = controller.listRecipes(-2, model, principal);

    assertThat(view).isEqualTo("recipes/list");
    assertThat(model.getAttribute("recipes")).isEqualTo(page.getContent());
    assertThat(model.getAttribute("currentPage")).isEqualTo(0);
    assertThat(model.getAttribute("aiSearchPrice")).isEqualTo(new BigDecimal("0.80"));
  }

  @Test
  void viewRecipeRedirectsWhenRecipeNotFound() {
    RecipeController controller = controller();
    ExtendedModelMap model = new ExtendedModelMap();
    doThrow(new RecipeNotFoundException("missing")).when(recipeService).getRecipeById(99L);

    String view = controller.viewRecipe(99L, model, null);

    assertThat(view).isEqualTo("redirect:/recipes");
  }

  @Test
  void createRecipeRedirectsToFormWhenValidationFails() {
    RecipeController controller = controller();
    RecipeCreateDto dto = new RecipeCreateDto();
    dto.setName("Soup");
    BeanPropertyBindingResult binding = new BeanPropertyBindingResult(dto, "recipeCreateDto");
    binding.rejectValue("name", "invalid", "invalid");
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

    String view = controller.createRecipe(dto, binding, redirect, null);

    assertThat(view).isEqualTo("redirect:/recipes/create");
    assertThat(redirect.getFlashAttributes()).containsKey("recipeCreateDto");
  }

  @Test
  void searchRecipesAiRedirectsToLoginWhenNoAuthenticatedUser() {
    RecipeController controller = controller();

    String view = controller.searchRecipes("cake", "ai", 0, "home", new ExtendedModelMap(), null);

    assertThat(view).isEqualTo("redirect:/login");
  }

  @Test
  void deleteRecipeRedirectsToRecipesWhenRefererMissing() {
    RecipeController controller = controller();
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    MockHttpServletRequest request = new MockHttpServletRequest();

    String view = controller.deleteRecipe(8L, redirect, request);

    assertThat(view).isEqualTo("redirect:/recipes");
    assertThat(redirect.getFlashAttributes().get("successMessage"))
        .isEqualTo("Recipe deleted successfully!");
  }

  private RecipeController controller() {
    return new RecipeController(
        recipeService,
        recipeReviewService,
        followRecipeService,
        likerService,
        superLikeService,
        userService,
        recipeAiService,
        configurationService,
        walletService,
        superLikeStrategyFactory
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
