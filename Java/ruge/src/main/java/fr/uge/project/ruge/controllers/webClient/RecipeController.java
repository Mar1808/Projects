package fr.uge.project.ruge.controllers.webClient;

import fr.uge.project.ruge.dto.RecipeCreateDto;
import fr.uge.project.ruge.dto.RecipeDetailDto;
import fr.uge.project.ruge.dto.RecipeVariantDto;
import fr.uge.project.ruge.dto.SimpleRecipeDto;
import fr.uge.project.ruge.exceptions.RecipeNotFoundException;
import fr.uge.project.ruge.exceptions.UserNotFoundException;
import fr.uge.project.ruge.factory.SuperLikeStrategyFactory;
import fr.uge.project.ruge.helper.CurrentUserSupport;
import fr.uge.project.ruge.services.ConfigurationService;
import fr.uge.project.ruge.services.FollowRecipeService;
import fr.uge.project.ruge.services.LikerService;
import fr.uge.project.ruge.services.RecipeAiService;
import fr.uge.project.ruge.services.RecipeReviewService;
import fr.uge.project.ruge.services.RecipeService;
import fr.uge.project.ruge.services.SuperLikeService;
import fr.uge.project.ruge.services.UserService;
import fr.uge.project.ruge.services.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Controller handling recipe web pages.
 */
@Slf4j
@Controller
@RequestMapping("/recipes")
public class RecipeController {

  private final RecipeService recipeService;
  private final RecipeReviewService recipeReviewService;
  private final FollowRecipeService followRecipeService;
  private final LikerService likerService;
  private final SuperLikeService superLikeService;
  private final UserService userService;
  private final RecipeAiService recipeAiService;
  private final ConfigurationService configurationService;
  private final WalletService walletService;
  private final SuperLikeStrategyFactory superLikeStrategyFactory;

  public RecipeController(RecipeService recipeService,
                          RecipeReviewService recipeReviewService,
                          FollowRecipeService followRecipeService,
                          LikerService likerService,
                          SuperLikeService superLikeService,
                          UserService userService,
                          RecipeAiService recipeAiService,
                          ConfigurationService configurationService,
                          WalletService walletService,
                          SuperLikeStrategyFactory superLikeStrategyFactory) {
    this.recipeService = Objects.requireNonNull(recipeService);
    this.recipeReviewService = Objects.requireNonNull(recipeReviewService);
    this.followRecipeService = Objects.requireNonNull(followRecipeService);
    this.likerService = Objects.requireNonNull(likerService);
    this.superLikeService = Objects.requireNonNull(superLikeService);
    this.userService = Objects.requireNonNull(userService);
    this.recipeAiService = Objects.requireNonNull(recipeAiService);
    this.configurationService = Objects.requireNonNull(configurationService);
    this.walletService = Objects.requireNonNull(walletService);
    this.superLikeStrategyFactory = Objects.requireNonNull(superLikeStrategyFactory);
  }

  /**
   * Displays the current user's recipes with pagination.
   *
   * @param page page number (default 0)
   * @param model Thymeleaf model
   * @return Thymeleaf view name
   */
  @GetMapping
  public String listRecipes(
          @RequestParam(defaultValue = "0") int page,
          Model model,
          Principal principal) {
    log.info("Displaying recipe list - page {}", page);
    if (principal == null) {
      return "redirect:/login";
    }
    if (page < 0) {
      page = 0;
    }
    var currentUser = userService.findByEmail(principal.getName()).orElseThrow();
    var recipePage = recipeService.getRecipesByCreatorIdPageable(currentUser.getId(), page);
    var recipes = recipePage.getContent();

    // Récupération du prix réel de la recherche IA pour CET utilisateur (Promotions appliquées)
    model.addAttribute("aiSearchPrice", walletService.getAiSearchPrice(currentUser));

    model.addAttribute("recipes", recipes);
    model.addAttribute("currentPage", page);
    model.addAttribute("totalPages", recipePage.getTotalPages());
    model.addAttribute("totalItems", recipePage.getTotalElements());
    model.addAttribute("hasNext", recipePage.hasNext());
    model.addAttribute("hasPrevious", recipePage.hasPrevious());
    return "recipes/list";
  }

  /**
   * Displays recipe details.
   *
   * @param id recipe identifier
   * @param model Thymeleaf model
   * @return Thymeleaf view name
   */
  @GetMapping("/{id}")
  public String viewRecipe(@PathVariable Long id,
                           Model model,
                           Authentication authentication) {
    log.info("Displaying recipe details: {}", id);
    try {
      var recipe = recipeService.getRecipeById(id);
      model.addAttribute("recipe", recipe);
      model.addAttribute("recipeReviews", recipeReviewService.getReviewsByRecipeId(id));
      model.addAttribute("likeCount", likerService.countLikes(id));
      model.addAttribute("superLikeCount", superLikeService.countSuperLikes(id));

      var isFollowingRecipe = false;
      var isLikedRecipe = false;
      var isOwnRecipe = false;
      var isSuperLikedRecipe = false;
      var currentUserOpt = CurrentUserSupport.currentUser(userService, authentication);

      // On met le prix de base par défaut si l'utilisateur n'est pas connecté
      BigDecimal actualSuperLikePrice = configurationService.getConfiguration().getSuperLikePrice();

      if (currentUserOpt.isPresent()) {
        var currentUser = currentUserOpt.get();

        // Calcul du VRAI prix à payer via la stratégie active (Promo standard ou 11ème like)
        actualSuperLikePrice = superLikeStrategyFactory.getStrategy()
                .calculateImpact(currentUser, 1).priceToPay();

        if (recipe.getCreatorId() != null) {
          isOwnRecipe = currentUser.getId().equals(recipe.getCreatorId());
        }
        if (!isOwnRecipe) {
          isFollowingRecipe = followRecipeService.isFollowing(currentUser.getId(), id);
          isLikedRecipe = likerService.hasLiked(currentUser.getId(), id);
          isSuperLikedRecipe = superLikeService.hasSuperLiked(currentUser.getId(), id);
        }
      }

      // On envoie le vrai prix calculé à la page HTML
      model.addAttribute("superLikePrice", actualSuperLikePrice);

      model.addAttribute("isFollowingRecipe", isFollowingRecipe);
      model.addAttribute("isLikedRecipe", isLikedRecipe);
      model.addAttribute("isOwnRecipe", isOwnRecipe);
      model.addAttribute("isSuperLikedRecipe", isSuperLikedRecipe);
      return "recipes/detail";
    } catch (RecipeNotFoundException e) {
      model.addAttribute("errorMessage", "Recipe not found");
      return "redirect:/recipes";
    }
  }

  /**
   * Displays the recipe creation form.
   *
   * @param model Thymeleaf model
   * @return Thymeleaf view name
   */
  @GetMapping("/create")
  public String showCreateForm(Model model, Authentication authentication) {
    var currentUserOpt = CurrentUserSupport.currentUser(userService, authentication);
    if (currentUserOpt.isEmpty()) {
      return "redirect:/login";
    }
    log.info("Displaying recipe creation form");
    if (!model.containsAttribute("recipeCreateDto")) {
      model.addAttribute("recipeCreateDto", new RecipeCreateDto());
    }
    return "recipes/create";
  }

  /**
   * Handles recipe creation form submission.
   *
   * @param createDto recipe payload
   * @param bindingResult validation result
   * @param redirectAttributes redirect attributes
   * @return redirect to list/detail or back to create form on error
   */
  @PostMapping("/create")
  public String createRecipe(
          @Valid @ModelAttribute("recipeCreateDto") RecipeCreateDto createDto,
          BindingResult bindingResult,
          RedirectAttributes redirectAttributes,
          Authentication authentication) {
    log.info("Recipe creation form submitted: {}", createDto.getName());
    if (bindingResult.hasErrors()) {
      log.warn("Validation errors while creating recipe");
      redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.recipeCreateDto", bindingResult);
      redirectAttributes.addFlashAttribute("recipeCreateDto", createDto);
      return "redirect:/recipes/create";
    }
    try {
      var currentUser = CurrentUserSupport.currentUser(userService, authentication).orElse(null);
      RecipeDetailDto createdRecipe = recipeService.createRecipe(createDto, currentUser);
      redirectAttributes.addFlashAttribute(
              "successMessage",
              "Recipe \"" + createdRecipe.getName() + "\" created successfully!"
      );
      log.info("Recipe created successfully, redirecting to detail page");
      return "redirect:/recipes/" + createdRecipe.getId();
    } catch (IllegalArgumentException e) {
      log.error("Business validation error while creating recipe", e);
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
      redirectAttributes.addFlashAttribute("recipeCreateDto", createDto);
      return "redirect:/recipes/create";
    } catch (Exception e) {
      log.error("Unexpected error while creating recipe", e);
      redirectAttributes.addFlashAttribute("errorMessage", "Error while creating recipe: " + e.getMessage());
      redirectAttributes.addFlashAttribute("recipeCreateDto", createDto);
      return "redirect:/recipes/create";
    }
  }

  @PostMapping("/{id}/follow")
  public String followRecipe(@PathVariable Long id,
                             RedirectAttributes redirectAttributes,
                             Authentication authentication) {
    var currentUser = CurrentUserSupport.currentUser(userService, authentication).orElse(null);
    if (currentUser == null) {
      return "redirect:/login";
    }
    try {
      followRecipeService.follow(currentUser.getId(), id);
      redirectAttributes.addFlashAttribute("successMessage", "Recipe followed.");
    } catch (IllegalArgumentException | RecipeNotFoundException | UserNotFoundException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/recipes/" + id;
  }

  @PostMapping("/{id}/unfollow")
  public String unfollowRecipe(@PathVariable Long id,
                               RedirectAttributes redirectAttributes,
                               Authentication authentication) {
    var currentUser = CurrentUserSupport.currentUser(userService, authentication).orElse(null);
    if (currentUser == null) {
      return "redirect:/login";
    }
    followRecipeService.unfollow(currentUser.getId(), id);
    redirectAttributes.addFlashAttribute("successMessage", "Recipe removed from favorites.");
    return "redirect:/recipes/" + id;
  }

  @PostMapping("/{id}/like")
  public String likeRecipe(@PathVariable Long id,
                           RedirectAttributes redirectAttributes,
                           Authentication authentication) {
    var currentUser = CurrentUserSupport.currentUser(userService, authentication).orElse(null);
    if (currentUser == null) {
      return "redirect:/login";
    }
    try {
      likerService.like(currentUser.getId(), id);
      redirectAttributes.addFlashAttribute("successMessage", "Recipe liked.");
    } catch (IllegalArgumentException | RecipeNotFoundException | UserNotFoundException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/recipes/" + id;
  }

  @PostMapping("/{id}/unlike")
  public String unlikeRecipe(@PathVariable Long id,
                             RedirectAttributes redirectAttributes,
                             Authentication authentication) {
    var currentUser = CurrentUserSupport.currentUser(userService, authentication).orElse(null);
    if (currentUser == null) {
      return "redirect:/login";
    }
    likerService.unlike(currentUser.getId(), id);
    redirectAttributes.addFlashAttribute("successMessage", "Like removed.");
    return "redirect:/recipes/" + id;
  }

  @PostMapping("/{id}/super-like")
  public String superLikeRecipe(@PathVariable Long id,
                                RedirectAttributes redirectAttributes,
                                Authentication authentication) {
    var currentUser = CurrentUserSupport.currentUser(userService, authentication).orElse(null);
    if (currentUser == null) {
      return "redirect:/login";
    }
    try {
      superLikeService.superLike(currentUser.getId(), id);
      redirectAttributes.addFlashAttribute("successMessage", "Super-like sent.");
    } catch (IllegalArgumentException | IllegalStateException | RecipeNotFoundException | UserNotFoundException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/recipes/" + id;
  }

  @PostMapping("/{id}/reviews")
  public String saveReviewComment(@PathVariable Long id,
                                  @RequestParam(name = "review", required = false) String reviewComment,
                                  RedirectAttributes redirectAttributes,
                                  Authentication authentication) {
    var currentUser = CurrentUserSupport.currentUser(userService, authentication).orElse(null);
    if (currentUser == null) {
      return "redirect:/login";
    }
    try {
      recipeReviewService.saveReviewComment(currentUser.getId(), id, reviewComment);
      redirectAttributes.addFlashAttribute("successMessage", "Review posted.");
    } catch (IllegalArgumentException | RecipeNotFoundException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/recipes/" + id;
  }

  @GetMapping("/search")
  public String searchRecipes(
          @RequestParam(name = "query", required = false) String query,
          @RequestParam(name = "type", defaultValue = "standard") String type,
          @RequestParam(name = "page", defaultValue = "0") int page,
          @RequestParam(defaultValue = "search") String sourcePage,
          Model model,
          Authentication authentication) {
    var effectiveQuery = query == null ? "" : query;

    // Récupération du prix de la recherche IA
    var currentUserOpt = CurrentUserSupport.currentUser(userService, authentication);
    if ("ai".equals(type) && currentUserOpt.isEmpty()) {
      return "redirect:/login";
    }

    List<SimpleRecipeDto> recipes = List.of();
    if (currentUserOpt.isPresent()) {
      model.addAttribute("aiSearchPrice", walletService.getAiSearchPrice(currentUserOpt.get()));
    } else {
      var configuration = configurationService.getConfiguration();
      if (configuration != null) {
        model.addAttribute("aiSearchPrice", configuration.getAiSearchPrice());
      }
    }

    if ("ai".equals(type)) {
      var currentUser = currentUserOpt.get();
      try {
        recipes = recipeAiService.askRecipe(currentUser, effectiveQuery);
        model.addAttribute("isAiSearch", true);
        model.addAttribute("totalPages", 1);
        model.addAttribute("totalItems", recipes.size());
      } catch (RuntimeException e) {
        log.warn("AI search failed: {}", e.getMessage());
        model.addAttribute("errorMessage", e.getMessage());
        model.addAttribute("isAiSearch", true);
        model.addAttribute("totalPages", 0);
        model.addAttribute("totalItems", 0);
      }
    } else {
      Page<SimpleRecipeDto> recipePage = recipeService.searchRecipe(effectiveQuery, page);
      recipes = recipePage.getContent();
      model.addAttribute("isAiSearch", false);
      model.addAttribute("totalPages", recipePage.getTotalPages());
      model.addAttribute("totalItems", recipePage.getTotalElements());
    }

    model.addAttribute("recipes", recipes);
    model.addAttribute("currentPage", page);
    model.addAttribute("query", effectiveQuery);
    return sourcePage.equals("my-recipes") ? "recipes/list" : "users/search";
  }

  @PostMapping("/delete/{id}")
  public String deleteRecipe(@PathVariable Long id,
                             RedirectAttributes redirectAttributes,
                             HttpServletRequest request) {
    recipeService.deleteRecipe(id);
    redirectAttributes.addFlashAttribute("successMessage", "Recipe deleted successfully!");
    var referer = request.getHeader("Referer");
    if (referer == null || referer.isBlank()) {
      return "redirect:/recipes";
    }
    return "redirect:" + referer;
  }

  /**
   * Displays the variant creation form.
   */
  @GetMapping("/{id}/variant")
  public String showVariantForm(@PathVariable Long id,
                                Model model) {
    RecipeDetailDto recipe = recipeService.getRecipeByIdForVariant(id);
    model.addAttribute("recipe", recipe);
    return "recipes/variant";
  }

  /**
   * Handles variant creation form submission.
   */
  @PostMapping("/{id}/variant")
  public String createVariant(@PathVariable Long id,
                              @Valid @ModelAttribute RecipeVariantDto variantDto,
                              BindingResult bindingResult,
                              Model model,
                              Authentication authentication) {
    if (bindingResult.hasErrors()) {
      RecipeDetailDto recipe = recipeService.getRecipeById(id);
      model.addAttribute("recipe", recipe);
      return "recipes/variant";
    }

    variantDto.setOriginalRecipeId(id);
    var currentUser = CurrentUserSupport.currentUser(userService, authentication).orElse(null);
    RecipeDetailDto created = recipeService.createVariant(variantDto, currentUser);
    return "redirect:/recipes/" + created.getId();
  }

  @GetMapping("/{id}/modify")
  public String showModifyForm(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
    var currentUserOpt = CurrentUserSupport.currentUser(userService, authentication);
    if (currentUserOpt.isEmpty()) {
      return "redirect:/login";
    }

    try {
      log.info("Displaying recipe update form");
      var recipe = recipeService.getRecipeByIdModifyRight(id, currentUserOpt.get());
      model.addAttribute("recipeDetailDto", recipe);
      return "recipes/modify";
    } catch (SecurityException e) {
      redirectAttributes.addAttribute("errorMessage", "Unauthorized access to modify this recipe.");
      return "redirect:/recipes/" + id;
    }
  }

  @PostMapping("/{id}/modify")
  public String modifyRecipe(
          @PathVariable Long id,
          @Valid @ModelAttribute("recipeDetailDto") RecipeDetailDto recipeDetailDto,
          BindingResult bindingResult,
          RedirectAttributes redirectAttributes,
          Authentication authentication) {
    log.info("Recipe modification form submitted: {}", recipeDetailDto.getName());

    if (bindingResult.hasErrors()) {
      log.warn("Validation errors while modifying recipe");
      var fieldErrors = bindingResult.getFieldErrors();
      var errorMessages = fieldErrors.stream()
              .map(error -> error.getField() + ": " + error.getDefaultMessage()).collect(Collectors.joining("\n"));
      redirectAttributes.addFlashAttribute("errorMessage", errorMessages);
      redirectAttributes.addFlashAttribute("recipeDetailDto", recipeDetailDto);
      return "redirect:/recipes/" + id + "/modify";
    }

    try {
      var currentUser = CurrentUserSupport.currentUser(userService, authentication).orElse(null);
      RecipeDetailDto updatedRecipe = recipeService.updateRecipe(id, recipeDetailDto, currentUser);
      redirectAttributes.addFlashAttribute(
              "successMessage",
              "Recipe \"" + updatedRecipe.getName() + "\" updated successfully!"
      );
      log.info("Recipe updated successfully, redirecting to detail page");
      return "redirect:/recipes/" + updatedRecipe.getId();
    } catch (SecurityException e) {
      log.error("Unauthorized modification attempt", e);
      redirectAttributes.addFlashAttribute("errorMessage", "You don't have permission to modify this recipe.");
      return "redirect:/recipes/" + id;
    } catch (RecipeNotFoundException e) {
      log.error("Recipe not found while modifying", e);
      redirectAttributes.addFlashAttribute("errorMessage", "Recipe not found.");
      return "redirect:/recipes";
    } catch (Exception e) {
      log.error("Unexpected error while modifying recipe", e);
      redirectAttributes.addFlashAttribute("errorMessage", "Error while updating recipe: " + e.getMessage());
      redirectAttributes.addFlashAttribute("recipeDetailDto", recipeDetailDto);
      return "redirect:/recipes/" + id + "/modify";
    }
  }

}