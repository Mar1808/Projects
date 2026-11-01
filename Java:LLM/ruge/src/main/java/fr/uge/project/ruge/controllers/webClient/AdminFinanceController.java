package fr.uge.project.ruge.controllers.webClient;

import fr.uge.project.ruge.dto.TransactionDto;
import fr.uge.project.ruge.services.ConfigurationService;
import fr.uge.project.ruge.services.SuperLikeService;
import fr.uge.project.ruge.services.TransactionService;
import fr.uge.project.ruge.services.WalletService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Controller
@RequestMapping("/admin/finances")
public class AdminFinanceController {
  private static final DateTimeFormatter TX_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS");

  private final WalletService walletService;
  private final TransactionService transactionService;
  private final ConfigurationService configurationService;
  private final SuperLikeService superLikeService;

  public AdminFinanceController(WalletService walletService,
                                TransactionService transactionService,
                                ConfigurationService configurationService,
                                SuperLikeService superLikeService) {
    this.transactionService = Objects.requireNonNull(transactionService);
    this.walletService = Objects.requireNonNull(walletService);
    this.configurationService = Objects.requireNonNull(configurationService);
    this.superLikeService = Objects.requireNonNull(superLikeService);
  }

  @GetMapping
  public String index(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
      @RequestParam(required = false) Long walletSourceId,
      @RequestParam(required = false) Long walletDestinationId,
      @RequestParam(defaultValue = "10") int limit,
      Model model)
  {
    var pageable = PageRequest.of(0, limit);
    var distributionAmount = walletService.getDistributionAmount();
    var benefitAmount = walletService.getBenefitAmount();
    model.addAttribute("distributionAmount", distributionAmount);
    model.addAttribute("benefitAmount", benefitAmount);
    model.addAttribute("beneficeAmount", benefitAmount);
    model.addAttribute("transactions", transactionService.getFilteredTransactions(startDate, endDate, walletSourceId, walletDestinationId, pageable));
    model.addAttribute("redistributionDetails", transactionService.getLatestRedistributionDetails(20));
    model.addAttribute("appConfig", configurationService.getConfiguration());
    model.addAttribute("currentLimit", limit);
    return "admin/manageFinances";
  }

  @GetMapping("/export-transactions")
  public void exportToCsv(HttpServletResponse response) throws IOException {
    response.setContentType("text/csv; charset=UTF-8");
    response.setHeader("Content-Disposition", "attachment; filename=\"transactions.csv\"");
    var transactions = transactionService.getAllTransactions();
    var writer = response.getWriter();
    writer.println("ID;Date;Amount;SourceWallet;DestinationWallet;Type;SourceRecipe;TargetRecipe;Description;ReferenceId");
    for (TransactionDto transaction : transactions) {
      var sourceId = transaction.getSourceUserId() != null ? String.valueOf(transaction.getSourceUserId()) : "";
      var targetId = transaction.getTargetUserId() != null ? String.valueOf(transaction.getTargetUserId()) : "";
      var sourceRecipeId = transaction.getSourceRecipeId() != null ? String.valueOf(transaction.getSourceRecipeId()) : "";
      var targetRecipeId = transaction.getTargetRecipeId() != null ? String.valueOf(transaction.getTargetRecipeId()) : "";
      var formattedDate = transaction.getDate() != null ? transaction.getDate().format(TX_DATE_FORMATTER) : "";
      writer.println(
          transaction.getId() + ";" +
              formattedDate + ";" +
              transaction.getAmount() + ";" +
              sourceId + ";" +
              targetId + ";" +
              (transaction.getTransactionType() != null ? transaction.getTransactionType() : "") + ";" +
              sourceRecipeId + ";" +
              targetRecipeId + ";" +
              transaction.getDescription() + ";" +
              transaction.getReferenceId()
      );
    }
  }

  @PostMapping("/update-prices")
  public String updatePrices(@RequestParam BigDecimal aiSearchPrice,
                             @RequestParam BigDecimal superLikePrice,
                             RedirectAttributes redirectAttributes) {
    try {
      configurationService.updatePrices(aiSearchPrice, superLikePrice);
      redirectAttributes.addFlashAttribute("successMessage", "Prices updated successfully.");
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("errorMessage", "Failed to update prices.");
    }
    return "redirect:/admin/finances";
  }

  @PostMapping("/redistribute-now")
  public String runRedistributionNow(RedirectAttributes redirectAttributes) {
    try {
      var result = superLikeService.redistributeAllPendingByMonth();
      if (result.processedSuperLikeCount() == 0) {
        redirectAttributes.addFlashAttribute("successMessage", "No pending super-likes to redistribute.");
      } else {
        redirectAttributes.addFlashAttribute(
            "successMessage",
            "Redistribution executed across " + result.processedMonthCount()
                + " month(s): " + result.processedSuperLikeCount() + " super-like(s), "
                + result.createdTransactionCount() + " transaction(s), total "
                + result.totalRedistributed() + " EUR."
        );
      }
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("errorMessage", "Redistribution failed: " + e.getMessage());
    }
    return "redirect:/admin/finances";
  }
}
