package fr.uge.project.ruge.controllers.apiRestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.uge.project.ruge.dto.*;
import fr.uge.project.ruge.exceptions.RecipeNotFoundException;
import fr.uge.project.ruge.factory.SuperLikeStrategyFactory;
import fr.uge.project.ruge.services.*;
import fr.uge.project.ruge.services.rest.JwtService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/recipes")
public class RecipeApiRestController {

  private final RecipeService recipeService;
  private final RecipeReviewService recipeReviewService;
  private final FollowRecipeService followRecipeService;
  private final LikerService likerService;
  private final SuperLikeService superLikeService;
  private final UserService userService;
  private final ConfigurationService configurationService;
  private final SuperLikeStrategyFactory superLikeStrategyFactory;
  private final JwtService jwtService;
  private final ObjectMapper objectMapper; // <-- AJOUT DU MAPPER
  private final RecipeAiService recipeAiService;

  public RecipeApiRestController(
          RecipeService recipeService,
          RecipeReviewService recipeReviewService,
          FollowRecipeService followRecipeService,
          LikerService likerService,
          SuperLikeService superLikeService,
          UserService userService,
          ConfigurationService configurationService,
          SuperLikeStrategyFactory superLikeStrategyFactory,
          JwtService jwtService,
          ObjectMapper objectMapper,
          RecipeAiService recipeAiService) { // <-- AJOUT DANS LE CONSTRUCTEUR
    this.recipeService = recipeService;
    this.recipeReviewService = recipeReviewService;
    this.followRecipeService = followRecipeService;
    this.likerService = likerService;
    this.superLikeService = superLikeService;
    this.userService = userService;
    this.configurationService = configurationService;
    this.superLikeStrategyFactory = superLikeStrategyFactory;
    this.jwtService = jwtService;
    this.objectMapper = objectMapper;
    this.recipeAiService = recipeAiService;
  }


  private String extractEmail(String authorizationHeader) {
    if (authorizationHeader == null || authorizationHeader.isBlank()) return null;
    String token = authorizationHeader.startsWith("Bearer ") ? authorizationHeader.substring(7) : authorizationHeader;
    if (!jwtService.validateToken(token)) return null;
    return jwtService.extractUsername(token);
  }

  @GetMapping
  public ResponseEntity<List<SimpleRecipeDto>> listRecipes(@RequestParam("page") int page) {
    log.info("Listing all recipes");
    return ResponseEntity.ok(recipeService.getAllRecipes(page));
  }

  @GetMapping("/search")
  public ResponseEntity<List<SimpleRecipeDto>> searchRecipe(@RequestParam("query") String query, @RequestParam(name = "type", defaultValue = "standard") String type, @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
    if ("ai".equalsIgnoreCase(type)) {
      log.info("Performing AI search for query: {}", query);
      var email = jwtService.extractUsername(authorizationHeader);
      var user = userService.findByEmail(email);
      if (user.isEmpty()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }
      return ResponseEntity.ok(recipeAiService.askRecipe(user.get(), query));
    }
    return ResponseEntity.ok(recipeService.searchRecipeListed(query));
  }

  @GetMapping("/{id}")
  public ResponseEntity<RecipeDetailWithInteractionsDto> getRecipe(
          @PathVariable Long id,
          @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
    var recipe = recipeService.getRecipeById(id);
    if (recipe == null) throw new RecipeNotFoundException("Recipe not found");

    var reviews   = recipeReviewService.getReviewsByRecipeId(id);
    long likeCount = likerService.countLikes(id);
    long superLikeCount = superLikeService.countSuperLikes(id);

    BigDecimal superLikePrice = configurationService.getConfiguration().getSuperLikePrice();
    boolean isOwnRecipe = false; boolean isLikedRecipe = false;
    boolean isFollowingRecipe = false; boolean isSuperLikedRecipe = false;

    String email = extractEmail(authorizationHeader);
    if (email != null) {
      var userOpt = userService.findByEmail(email);
      if (userOpt.isPresent()) {
        var currentUser = userOpt.get();
        superLikePrice = superLikeStrategyFactory.getStrategy().calculateImpact(currentUser, 1).priceToPay();

        if (recipe.getCreatorId() != null) isOwnRecipe = currentUser.getId().equals(recipe.getCreatorId());
        if (!isOwnRecipe) {
          isLikedRecipe      = likerService.hasLiked(currentUser.getId(), id);
          isFollowingRecipe  = followRecipeService.isFollowing(currentUser.getId(), id);
          isSuperLikedRecipe = superLikeService.hasSuperLiked(currentUser.getId(), id);
        }
      }
    }

    var dto = new RecipeDetailWithInteractionsDto(recipe.getId(), recipe.getName(), recipe.getDescription(), recipe.getImageUrl(), recipe.getCreatorName(), recipe.getCreatorId(), recipe.getSteps(), recipe.getIngredients(), recipe.getTotalDuration(), recipe.getRecipeSourceId(), recipe.isActive(), likeCount, superLikeCount, superLikePrice, isOwnRecipe, isLikedRecipe, isFollowingRecipe, isSuperLikedRecipe, reviews);
    return ResponseEntity.ok(dto);
  }

  @PostMapping("/{id}/like")
  public ResponseEntity<String> likeRecipe(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String auth) {
    String email = extractEmail(auth);
    if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
    var userOpt = userService.findByEmail(email);
    if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
    try {
      likerService.like(userOpt.get().getId(), id);
      return ResponseEntity.ok("Recipe liked.");
    } catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
  }

  @PostMapping("/{id}/unlike")
  public ResponseEntity<String> unlikeRecipe(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String auth) {
    String email = extractEmail(auth);
    if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
    var userOpt = userService.findByEmail(email);
    if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
    likerService.unlike(userOpt.get().getId(), id);
    return ResponseEntity.ok("Like removed.");
  }

  @PostMapping("/{id}/follow")
  public ResponseEntity<String> followRecipe(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String auth) {
    String email = extractEmail(auth);
    if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
    var userOpt = userService.findByEmail(email);
    if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
    try {
      followRecipeService.follow(userOpt.get().getId(), id);
      return ResponseEntity.ok("Recipe followed.");
    } catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
  }

  @PostMapping("/{id}/unfollow")
  public ResponseEntity<String> unfollowRecipe(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String auth) {
    String email = extractEmail(auth);
    if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
    var userOpt = userService.findByEmail(email);
    if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
    followRecipeService.unfollow(userOpt.get().getId(), id);
    return ResponseEntity.ok("Recipe removed from favorites.");
  }

  @PostMapping("/{id}/super-like")
  public ResponseEntity<String> superLikeRecipe(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String auth) {
    String email = extractEmail(auth);
    if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
    var userOpt = userService.findByEmail(email);
    if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
    try {
      superLikeService.superLike(userOpt.get().getId(), id);
      return ResponseEntity.ok("Super-like sent.");
    } catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
  }

  @PostMapping("/{id}/reviews")
  public ResponseEntity<String> postReview(@PathVariable Long id, @RequestParam(name = "review", required = false) String reviewComment, @RequestHeader(value = "Authorization", required = false) String auth) {
    String email = extractEmail(auth);
    if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
    var userOpt = userService.findByEmail(email);
    if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
    try {
      recipeReviewService.saveReviewComment(userOpt.get().getId(), id, reviewComment);
      return ResponseEntity.ok("Review posted.");
    } catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
  }


  @PostMapping(value = {"", "/create"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> createRecipe(
          @RequestPart("recipe") String recipeJson,
          @RequestPart(value = "image", required = false) MultipartFile imageFile,
          @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

    if (authorizationHeader == null || authorizationHeader.isBlank()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing Authorization header");
    }

    String email = extractEmail(authorizationHeader);
    if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

    var userOpt = userService.findByEmail(email);
    if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");

    try {
      RecipeCreateDto createDto = objectMapper.readValue(recipeJson, RecipeCreateDto.class);

      RecipeDetailDto createdRecipe = recipeService.createRecipe(createDto, userOpt.get());
      return ResponseEntity.status(HttpStatus.CREATED).body(createdRecipe);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      return ResponseEntity.badRequest().body("Invalid recipe JSON format: " + e.getMessage());
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (Exception e) {
      log.error("Failed to create recipe via REST API", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create recipe");
    }
  }

  @PostMapping("/{id}/variant")
  public ResponseEntity<?> createVariant(
          @PathVariable Long id,
          @Valid @RequestBody RecipeVariantDto variantDto,
          @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

    String email = extractEmail(authorizationHeader);
    if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

    var userOpt = userService.findByEmail(email);
    if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");

    try {
      variantDto.setOriginalRecipeId(id);
      RecipeDetailDto createdVariant = recipeService.createVariant(variantDto, userOpt.get());
      return ResponseEntity.status(HttpStatus.CREATED).body(createdVariant);
    } catch (IllegalArgumentException | RecipeNotFoundException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (Exception e) {
      log.error("Failed to create variant via REST API for original recipe {}", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create variant");
    }
  }
}