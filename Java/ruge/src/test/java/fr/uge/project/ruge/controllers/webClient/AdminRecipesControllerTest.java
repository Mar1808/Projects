package fr.uge.project.ruge.controllers.webClient;

import fr.uge.project.ruge.dto.RecipeDetailDto;
import fr.uge.project.ruge.services.RecipeService;
import fr.uge.project.ruge.testsupport.MockitoTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRecipesControllerTest extends MockitoTestSupport {
  @Mock
  private RecipeService recipeService;

  @Test
  void indexReturnsManageRecipesViewWithRecipes() {
    AdminRecipesController controller = new AdminRecipesController(recipeService);
    ExtendedModelMap model = new ExtendedModelMap();
    var recipes = List.of(RecipeDetailDto.builder().id(1L).name("Cake").build());
    when(recipeService.getAllDetailedRecipes()).thenReturn(recipes);

    String view = controller.index(model);

    assertThat(view).isEqualTo("admin/manageRecipes");
    assertThat(model.getAttribute("recipes")).isEqualTo(recipes);
  }
}
