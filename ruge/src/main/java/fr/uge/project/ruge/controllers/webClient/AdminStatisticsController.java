package fr.uge.project.ruge.controllers.webClient;

import fr.uge.project.ruge.services.AdminStatisticsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Objects;

@Controller
@RequestMapping("/admin/statistics")
public class AdminStatisticsController {
  private final AdminStatisticsService adminStatisticsService;

  public AdminStatisticsController(AdminStatisticsService adminStatisticsService) {
    this.adminStatisticsService = Objects.requireNonNull(adminStatisticsService);
  }

  @GetMapping
  public String index(Model model) {
    model.addAttribute("statistics", adminStatisticsService.getStatistics());
    return "admin/statistics";
  }
}
