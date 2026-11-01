package fr.uge.project.ruge.controllers.apiRestClient;

import fr.uge.project.ruge.dto.ProfilePhotoDto;
import fr.uge.project.ruge.dto.SimpleRecipeDto;
import fr.uge.project.ruge.dto.UserStatsResponse;
import fr.uge.project.ruge.services.LikerService;
import fr.uge.project.ruge.services.RecipeReviewService;
import fr.uge.project.ruge.services.RecipeService;
import fr.uge.project.ruge.services.UserService;
import fr.uge.project.ruge.services.rest.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/users/profile")
public class UserProfileApiRestController {
  private final RecipeService recipeService;
  private final UserService userService;
  private final JwtService jwtService;
  private final RecipeReviewService recipeReviewService;
  private final LikerService likerService;

  public UserProfileApiRestController(RecipeService recipeService,
                                      UserService userService,
                                      JwtService jwtService, RecipeReviewService recipeReviewService, LikerService likerService) {
    this.recipeService = recipeService;
    this.userService = userService;
    this.jwtService = jwtService;
    this.likerService = Objects.requireNonNull(likerService);
    this.recipeReviewService = Objects.requireNonNull(recipeReviewService);
  }

  @GetMapping("/my-recipes")
  public ResponseEntity<List<SimpleRecipeDto>> getMyRecipes(
      @RequestHeader(value = "Authorization") String authorizationHeader) {
    log.info("Request for user recipes");
    var isAuthorized = jwtService.validateToken(authorizationHeader);
    if (!isAuthorized) {
      return ResponseEntity.status(401).build();
    }
    var email = jwtService.extractUsername(authorizationHeader);
    log.info("Fetching recipes for user: {}", email);
    var user = userService.findByEmail(email);
    if (user.isPresent()) {
      var recipes = recipeService.getRecipesByCreatorId(user.get().getId());
      return ResponseEntity.ok(recipes);
    }
    return ResponseEntity.status(404).build();
  }

  @GetMapping("/photos")
  public ResponseEntity<List<ProfilePhotoDto>> getProfilePhotos() {
    return ResponseEntity.ok(userService.getAvailableProfilePhotos());
  }

  @PutMapping("/photo")
  public ResponseEntity<String> updateMyProfilePhoto(
      @RequestHeader(value = "Authorization") String authorizationHeader,
      @RequestParam("profilePhotoId") Long profilePhotoId) {
    var isAuthorized = jwtService.validateToken(authorizationHeader);
    if (!isAuthorized) {
      return ResponseEntity.status(401).build();
    }
    var email = jwtService.extractUsername(authorizationHeader);
    try {
      userService.updateProfilePhotoByEmail(email, profilePhotoId);
      return ResponseEntity.ok("Profile photo updated");
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }


  @GetMapping("/{id}/stats")
  public UserStatsResponse getUserStats(@PathVariable Long id) {
    log.info("Fetching user stats for user: {}", id);
    var reviewReactions = recipeReviewService.getReviewsByAuthorId(id);
    var likeReactions = likerService.getLikesBySenderId(id);

    return new UserStatsResponse(
            reviewReactions.size(),
            likeReactions.size()
    );
  }
}
