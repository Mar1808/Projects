package fr.uge.project.ruge.controllers.webClient;

import fr.uge.project.ruge.services.ConfigurationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

@Controller
@RequestMapping("/admin/terms")
public class AdminTermsController {
  private final ConfigurationService configurationService;

  public AdminTermsController(ConfigurationService configurationService) {
    this.configurationService = Objects.requireNonNull(configurationService);
  }

  @GetMapping
  public String index(Model model) {
    model.addAttribute("termsContent", configurationService.getTermsContent());
    return "admin/manageTerms";
  }

  @PostMapping
  public String updateTerms(@RequestParam String termsContent, RedirectAttributes redirectAttributes) {
    try {
      configurationService.updateTermsContent(termsContent);
      redirectAttributes.addFlashAttribute("successMessage", "Terms updated successfully.");
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("errorMessage", "Failed to update terms.");
    }
    return "redirect:/admin/terms";
  }
}
