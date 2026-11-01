package fr.uge.project.ruge.controllers.apiRestClient;

import fr.uge.project.ruge.dto.*;
import fr.uge.project.ruge.enums.Role;
import fr.uge.project.ruge.services.*;
import fr.uge.project.ruge.services.rest.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminApiRestController {
  private final UserService userService;
  private final JwtService jwtService;
  private final WalletService walletService;
  private final TransactionService transactionService;
  private final ConfigurationService configurationService;
  private final SuperLikeService superLikeService;
  private final AdminStatisticsService adminStatisticsService;
  private final RecipeService recipeService;
  private final PromotionService promotionService;

  public AdminApiRestController(
          UserService userService,
          JwtService jwtService,
          WalletService walletService,
          TransactionService transactionService,
          ConfigurationService configurationService,
          SuperLikeService superLikeService,
          AdminStatisticsService adminStatisticsService,
          RecipeService recipeService,
          PromotionService promotionService) {
    this.superLikeService = superLikeService;
    this.userService = userService;
    this.jwtService = jwtService;
    this.walletService = walletService;
    this.transactionService = transactionService;
    this.configurationService = configurationService;
    this.adminStatisticsService = adminStatisticsService;
    this.recipeService = recipeService;
    this.promotionService = promotionService;
  }

  @GetMapping("/users")
  public ResponseEntity<List<UserDisplayDto>> listUsers(@RequestHeader(value = "Authorization") String authorizationHeader) {
    if (!jwtService.validateToken(authorizationHeader)) {
      return ResponseEntity.status(401).build();
    }
    var email = jwtService.extractUsername(authorizationHeader);
    var adminUser = userService.findByEmail(email);
    if (adminUser.isEmpty() || adminUser.get().getRole() != Role.ADMIN) {
      return ResponseEntity.status(403).build();
    }

    var users = userService.getAllUsers();
    return ResponseEntity.ok(users);
  }

  @GetMapping("/statistics")
  public ResponseEntity<AdminStatisticsDto> getStatistics(@RequestHeader(value = "Authorization") String authorizationHeader) {
    var isAuthorized = jwtService.validateToken(authorizationHeader);
    if (!isAuthorized) {
      return ResponseEntity.status(401).build();
    }
    var email = jwtService.extractUsername(authorizationHeader);
    var adminUser = userService.findByEmail(email);
    if (adminUser.isEmpty()) {
      return ResponseEntity.status(404).body(null);
    }
    if (adminUser.get().getRole() != Role.ADMIN) {
      return ResponseEntity.status(403).body(null);
    }

    return ResponseEntity.ok(adminStatisticsService.getStatistics());
  }

  @GetMapping("/users/{id}/ban-history")
  public ResponseEntity<UserHistoryResponseDto> getBanHistory(
          @PathVariable Long id,
          @RequestHeader(value = "Authorization") String authorizationHeader) {
    if (!jwtService.validateToken(authorizationHeader)) {
      return ResponseEntity.status(401).build();
    }
    var email = jwtService.extractUsername(authorizationHeader);
    var adminUser = userService.findByEmail(email);
    if (adminUser.isEmpty() || adminUser.get().getRole() != Role.ADMIN) {
      return ResponseEntity.status(403).build();
    }

    var user = userService.getByIdOrThrow(id);
    var banHistory = userService.getBanHistory(id);

    var userDto = new UserDisplayDto(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getImageUrl(),
            user.getRole(),
            user.getStatus()
    );
    var responseBody = new UserHistoryResponseDto(userDto, banHistory);
    return ResponseEntity.ok(responseBody);
  }

  @PostMapping("/users/{id}/ban")
  public ResponseEntity<String> banUser(@PathVariable Long id,
                                        @RequestBody BanRequestDto request,
                                        @RequestHeader(value = "Authorization") String authorizationHeader) {
    log.info("Request to ban user with id {} received with reason: {} and duration: {} days", id, request.reason(), request.durationDays());
    var isAuthorized = jwtService.validateToken(authorizationHeader);
    if (!isAuthorized) {
      return ResponseEntity.status(401).build();
    }
    var email = jwtService.extractUsername(authorizationHeader);
    var adminUser = userService.findByEmail(email);
    if (adminUser.isEmpty()) {
      return ResponseEntity.status(404).body("Admin user not found");
    }
    if (adminUser.get().getId().equals(id)) {
      return ResponseEntity.badRequest().body("You cannot ban yourself");
    }
    if(adminUser.get().getRole() != Role.ADMIN) {
      return ResponseEntity.status(403).body("You do not have permission to ban users");
    }
    userService.banUser(id, adminUser.get().getId(), request.reason(), request.durationDays());
    return ResponseEntity.ok("User banned successfully");
  }

  @PostMapping("/users/{id}/unban")
  public ResponseEntity<String> unbanUser(@PathVariable Long id, @RequestHeader(value = "Authorization") String authorizationHeader) {
    var isAuthorized = jwtService.validateToken(authorizationHeader);
    if (!isAuthorized) {
      return ResponseEntity.status(401).build();
    }
    var email = jwtService.extractUsername(authorizationHeader);
    var adminUser = userService.findByEmail(email);
    if (adminUser.isEmpty()) {
      return ResponseEntity.status(404).body("Admin user not found");
    }
    if (adminUser.get().getId().equals(id)) {
      return ResponseEntity.badRequest().body("You cannot unban yourself");
    }
    if(adminUser.get().getRole() != Role.ADMIN) {
      return ResponseEntity.status(403).body("You do not have permission to unban users");
    }
    userService.unban(id);
    return ResponseEntity.ok("User unbanned successfully");
  }

  @GetMapping("/finances/dashboard")
  public ResponseEntity<FinancesDashboardDto> getAdminDashboard(@RequestHeader(value = "Authorization") String authorizationHeader) {
    var isAuthorized = jwtService.validateToken(authorizationHeader);
    if (!isAuthorized) {
      return ResponseEntity.status(401).build();
    }
    var email = jwtService.extractUsername(authorizationHeader);
    var adminUser = userService.findByEmail(email);
    if (adminUser.isEmpty() || adminUser.get().getRole() != Role.ADMIN) {
      return ResponseEntity.status(403).body(null);
    }

    var appConfig = configurationService.getConfiguration();
    var dashboardData = new FinancesDashboardDto(
            walletService.getDistributionAmount().doubleValue(),
            walletService.getBenefitAmount().doubleValue(),
            appConfig.getAiSearchPrice().doubleValue(),
            appConfig.getSuperLikePrice().doubleValue(),
            transactionService.getNLatestTransactions(10)
    );

    return ResponseEntity.ok(dashboardData);
  }

  @PostMapping("/finances/update-prices")
  public ResponseEntity<String> updatePrices(
          @RequestBody UpdatePricesDto updatePrices,
          @RequestHeader(value = "Authorization") String authorizationHeader
  ) {
    var isAuthorized = jwtService.validateToken(authorizationHeader);
    if (!isAuthorized) {
      return ResponseEntity.status(401).build();
    }
    var email = jwtService.extractUsername(authorizationHeader);
    var adminUser = userService.findByEmail(email);
    if (adminUser.isEmpty()) {
      return ResponseEntity.status(404).body("Admin user not found");
    }
    if(adminUser.get().getRole() != Role.ADMIN) {
      return ResponseEntity.status(403).body("You do not have permission to update prices");
    }

    try {
      configurationService.updatePrices(BigDecimal.valueOf(updatePrices.aiPrice()), BigDecimal.valueOf(updatePrices.superLikePrice()));
      return ResponseEntity.ok("Prices updated successfully");
    } catch (Exception e) {
      return ResponseEntity.status(500).body("An error occurred while updating prices: " + e.getMessage());
    }
  }

  @PostMapping("/finances/redistribution")
  public ResponseEntity<String> triggerRedistribution(@RequestHeader(value = "Authorization") String authorizationHeader) {
    var isAuthorized = jwtService.validateToken(authorizationHeader);
    if (!isAuthorized) {
      return ResponseEntity.status(401).build();
    }
    var email = jwtService.extractUsername(authorizationHeader);
    var adminUser = userService.findByEmail(email);
    if (adminUser.isEmpty() || adminUser.get().getRole() != Role.ADMIN) {
      return ResponseEntity.status(403).body("You do not have permission to trigger redistribution");
    }

    try {
      superLikeService.redistributeAllPendingByMonth();
      return ResponseEntity.ok("Redistribution triggered successfully");
    } catch (Exception e) {
      return ResponseEntity.status(500).body("An error occurred while triggering redistribution: " + e.getMessage());
    }
  }

  @GetMapping("/recipes/allDetailedRecipes")
  public ResponseEntity<List<RecipeDetailDto>> getAllDetailedRecipes(@RequestHeader(value = "Authorization") String authorizationHeader) {
    var isAuthorized = jwtService.validateToken(authorizationHeader);
    if (!isAuthorized) {
      return ResponseEntity.status(401).build();
    }
    var email = jwtService.extractUsername(authorizationHeader);
    var adminUser = userService.findByEmail(email);
    if (adminUser.isEmpty() || adminUser.get().getRole() != Role.ADMIN) {
      return ResponseEntity.status(403).body(null);
    }

    var recipes = recipeService.getAllDetailedRecipes();
    return ResponseEntity.ok(recipes);
  }

  @PostMapping("/recipes/{id}/delete")
  public ResponseEntity<Boolean> deleteRecipe(@PathVariable Long id, @RequestHeader(value = "Authorization") String authorizationHeader) {
    var isAuthorized = jwtService.validateToken(authorizationHeader);
    if (!isAuthorized) {
      return ResponseEntity.status(401).build();
    }
    var email = jwtService.extractUsername(authorizationHeader);
    var adminUser = userService.findByEmail(email);
    if (adminUser.isEmpty() || adminUser.get().getRole() != Role.ADMIN) {
      return ResponseEntity.status(403).body(false);
    }
    try {
      recipeService.deleteRecipe(id);
      return ResponseEntity.ok(true);
    } catch (Exception e) {
      log.error("Error deleting recipe with id {}: {}", id, e.getMessage());
      return ResponseEntity.status(500).body(false);
    }
  }

  // ════════════════════════════════════════════
  // AJOUT : ROUTES POUR GÉRER LES PROMOTIONS VIA L'API
  // ════════════════════════════════════════════

  // ════════════════════════════════════════════
// ROUTES POUR GÉRER LES PROMOTIONS VIA L'API
// ════════════════════════════════════════════

  @GetMapping("/promotions")
  public ResponseEntity<?> getPromotions(@RequestHeader(value = "Authorization") String authorizationHeader) {
    // Accessible à TOUS les utilisateurs connectés (pour que l'appli mobile puisse afficher le prix promo)
    if (!jwtService.validateToken(authorizationHeader)) {
      return ResponseEntity.status(401).build();
    }

    return ResponseEntity.ok(promotionService.getAllPromotions());
  }

  @PostMapping("/promotions")
  public ResponseEntity<?> createPromotion(
          @RequestParam String type,
          @RequestParam BigDecimal promoPrice,
          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
          @RequestHeader(value = "Authorization") String authorizationHeader) {
    // Réservé aux ADMINS
    if (!jwtService.validateToken(authorizationHeader)) return ResponseEntity.status(401).build();
    var email = jwtService.extractUsername(authorizationHeader);
    var adminUser = userService.findByEmail(email);
    if (adminUser.isEmpty() || adminUser.get().getRole() != Role.ADMIN) {
      return ResponseEntity.status(403).build();
    }

    try {
      promotionService.createPromotion(type, promoPrice, startDate, endDate);
      return ResponseEntity.ok("Promotion créée avec succès");
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  @PostMapping("/promotions/{id}/delete")
  public ResponseEntity<?> deletePromotion(
          @PathVariable Long id,
          @RequestHeader(value = "Authorization") String authorizationHeader) {
    // Réservé aux ADMINS
    if (!jwtService.validateToken(authorizationHeader)) return ResponseEntity.status(401).build();
    var email = jwtService.extractUsername(authorizationHeader);
    var adminUser = userService.findByEmail(email);
    if (adminUser.isEmpty() || adminUser.get().getRole() != Role.ADMIN) {
      return ResponseEntity.status(403).build();
    }

    try {
      promotionService.deletePromotion(id);
      return ResponseEntity.ok("Promotion supprimée avec succès");
    } catch (IllegalStateException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }
}