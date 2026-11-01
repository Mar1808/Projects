package fr.uge.project.ruge.controllers.webClient;

import fr.uge.project.ruge.helper.CurrentUserSupport;
import fr.uge.project.ruge.services.ConfigurationService;
import fr.uge.project.ruge.services.RecipeService;
import fr.uge.project.ruge.services.UserService;
import fr.uge.project.ruge.services.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class WebClientController {
  private final RecipeService recipeService;
  private final ConfigurationService configurationService;
  private final WalletService walletService;
  private final UserService userService;

  @GetMapping("/")
  public String root() {
    return "redirect:/home";
  }

  @GetMapping("/home")
  public String home(
          @RequestParam(name = "page", defaultValue = "0") int page,
      Model model,
      Authentication authentication
  ) {
    var recipePage = recipeService.getRecipes(page);
    var recipes = recipePage.getContent();

    model.addAttribute("recipes", recipes);
    model.addAttribute("query", null);
    model.addAttribute("resultCount", recipePage.getTotalElements());
    model.addAttribute("currentPage", page);
    model.addAttribute("totalPages", recipePage.getTotalPages());
    model.addAttribute("totalItems", recipePage.getTotalElements());
    model.addAttribute("isAiSearch", false);
    var currentUserOpt = CurrentUserSupport.currentUser(userService, authentication);
    if (currentUserOpt.isPresent()) {
      model.addAttribute("aiSearchPrice", walletService.getAiSearchPrice(currentUserOpt.get()));
    } else {
      model.addAttribute("aiSearchPrice", configurationService.getConfiguration().getAiSearchPrice());
    }
    return "users/home";
  }

  @GetMapping("terms")
  public String terms(Model model) {
    model.addAttribute("termsContent", configurationService.getTermsContent());
    return "users/terms";
  }
}
