package fr.uge.project.ruge.controllers.webClient;

import fr.uge.project.ruge.dto.UserProfileDto;
import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.exceptions.UserNotFoundException;
import fr.uge.project.ruge.helper.CurrentUserSupport;
import fr.uge.project.ruge.services.FollowService;
import fr.uge.project.ruge.services.LikerService;
import fr.uge.project.ruge.services.RecipeReviewService;
import fr.uge.project.ruge.services.RecipeService;
import fr.uge.project.ruge.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/users")
public class UserProfileController {
  private final RecipeService recipeService;
  private final RecipeReviewService recipeReviewService;
  private final LikerService likerService;
  private final FollowService followService;
  private final UserService userService;

  public UserProfileController(RecipeService recipeService,
                               RecipeReviewService recipeReviewService,
                               LikerService likerService,
                               FollowService followService,
                               UserService userService) {
    this.recipeService = Objects.requireNonNull(recipeService);
    this.recipeReviewService = Objects.requireNonNull(recipeReviewService);
    this.likerService = Objects.requireNonNull(likerService);
    this.followService = Objects.requireNonNull(followService);
    this.userService = Objects.requireNonNull(userService);
  }

  @GetMapping("/{id}")
  public String viewProfile(@PathVariable Long id,
                            Model model,
                            Authentication authentication) {
    UserEntity user;
    try {
      user = userService.getByIdOrThrow(id);
    } catch (UserNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    var recipes = recipeService.getRecipesByCreatorId(id);
    var reviewReactions = recipeReviewService.getReviewsByAuthorId(id);
    var likeReactions = likerService.getLikesBySenderId(id);
    var profile = new UserProfileDto(user.getId(), user.getName(), user.getImageUrl(), recipes);

    boolean isSelf = false;
    boolean isFollowing = false;
    var currentUser = CurrentUserSupport.currentUser(userService, authentication).orElse(null);
    if (currentUser != null) {
      isSelf = currentUser.getId().equals(user.getId());
      if (!isSelf) {
        isFollowing = followService.isFollowing(currentUser.getId(), user.getId());
      }
    }

    model.addAttribute("profile", profile);
    model.addAttribute("isSelf", isSelf);
    model.addAttribute("isFollowing", isFollowing);
    model.addAttribute("profilePhotos", isSelf ? userService.getAvailableProfilePhotos() : List.of());
    model.addAttribute("selectedProfilePhotoId", user.getProfilePhoto() != null ? user.getProfilePhoto().getId() : null);
    model.addAttribute("recipeCount", recipes.size());
    model.addAttribute("reviewReactions", reviewReactions);
    model.addAttribute("likeReactions", likeReactions);
    model.addAttribute("reviewCount", reviewReactions.size());
    model.addAttribute("likeCount", likeReactions.size());
    return "users/profile";
  }

  @PostMapping("/{id}/follow")
  public String follow(@PathVariable Long id,
                       RedirectAttributes redirectAttributes,
                       Authentication authentication) {
    var currentUser = CurrentUserSupport.currentUser(userService, authentication).orElse(null);
    if (currentUser == null) {
      return "redirect:/login";
    }
    try {
      followService.follow(currentUser.getId(), id);
      redirectAttributes.addFlashAttribute("successMessage", "You are now following this user.");
    } catch (IllegalArgumentException | UserNotFoundException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/users/" + id;
  }

  @PostMapping("/{id}/unfollow")
  public String unfollow(@PathVariable Long id,
                         RedirectAttributes redirectAttributes,
                         Authentication authentication) {
    var currentUser = CurrentUserSupport.currentUser(userService, authentication).orElse(null);
    if (currentUser == null) {
      return "redirect:/login";
    }
    followService.unfollow(currentUser.getId(), id);
    redirectAttributes.addFlashAttribute("successMessage", "You no longer follow this user.");
    return "redirect:/users/" + id;
  }

  @PostMapping("/{id}/photo")
  public String updateProfilePhoto(@PathVariable Long id,
                                   @RequestParam("profilePhotoId") Long profilePhotoId,
                                   RedirectAttributes redirectAttributes,
                                   Authentication authentication) {
    var currentUser = CurrentUserSupport.currentUser(userService, authentication).orElse(null);
    if (currentUser == null) {
      return "redirect:/login";
    }
    if (!currentUser.getId().equals(id)) {
      redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized action.");
      return "redirect:/users/" + id;
    }
    try {
      userService.updateProfilePhoto(id, profilePhotoId);
      redirectAttributes.addFlashAttribute("successMessage", "Profile photo updated.");
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/users/" + id;
  }
}
