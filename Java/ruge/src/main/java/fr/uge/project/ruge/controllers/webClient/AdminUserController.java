package fr.uge.project.ruge.controllers.webClient;

import fr.uge.project.ruge.services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Objects;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {
  private final UserService userService;

  public AdminUserController(UserService userService) {
    this.userService = Objects.requireNonNull(userService);
  }

  @GetMapping
  public String index(Model model) {
    var users = userService.getAllUsers();
    model.addAttribute("users", users);
    return "admin/adminPage";
  }

  @PostMapping("/{id}/ban")
  public String banUser(@PathVariable("id") Long targetUserId,
                        @RequestParam("reason") String reason,
                        @RequestParam(value = "duration", required = false) Integer duration,
                        @RequestParam("unit") String unit,
                        Principal principal,
                        RedirectAttributes redirectAttributes) {
    int finalDurationInDays = -1;
    if (!"permanent".equals(unit)) {
      if (duration == null || duration <= 0) {
        redirectAttributes.addFlashAttribute("errorMessage", "Please provide a valid duration.");
        return "redirect:/admin/users";
      }
      if ("days".equals(unit) || "hours".equals(unit)) {
        finalDurationInDays = duration;
      }
    }

    var adminEmail = principal.getName();
    var adminUser = userService.findByEmail(adminEmail);
    var adminId = adminUser.orElseThrow().getId();
    if (adminId.equals(targetUserId)) {
      redirectAttributes.addFlashAttribute("errorMessage", "You cannot ban yourself.");
      return "redirect:/admin/users";
    }

    userService.banUser(targetUserId, adminId, reason, finalDurationInDays);
    redirectAttributes.addFlashAttribute("successMessage", "User banned successfully.");
    return "redirect:/admin/users";
  }

  @PostMapping("/{id}/unban")
  public String unbanUser(@PathVariable Long id,
                          RedirectAttributes redirectAttributes) {
    userService.unban(id);
    redirectAttributes.addFlashAttribute("successMessage", "User unbanned successfully.");
    return "redirect:/admin/users";
  }

  @GetMapping("/{id}/history")
  public String getBanHistory(@PathVariable Long id, Model model) {
    var user = userService.getByIdOrThrow(id);
    var banHistory = userService.getBanHistory(id);
    model.addAttribute("banHistory", banHistory);
    model.addAttribute("user", user);
    return "admin/banHistory";
  }
}
