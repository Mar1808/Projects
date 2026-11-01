package fr.uge.project.ruge.controllers.webClient;

import fr.uge.project.ruge.services.RecipeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Objects;

@Controller
@RequestMapping("/admin/recipes")
public class AdminRecipesController {
  private final RecipeService recipeService;

  public AdminRecipesController(RecipeService recipeService) {
    this.recipeService = Objects.requireNonNull(recipeService);
  }

  @GetMapping
  public String index(Model model) {
    var recipes = recipeService.getAllDetailedRecipes();
    model.addAttribute("recipes", recipes);
    return "admin/manageRecipes";
  }
}
