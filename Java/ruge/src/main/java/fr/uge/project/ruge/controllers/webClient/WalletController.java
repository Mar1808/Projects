package fr.uge.project.ruge.controllers.webClient;

import fr.uge.project.ruge.helper.CurrentUserSupport;
import fr.uge.project.ruge.services.StripePaymentService;
import fr.uge.project.ruge.services.SuperLikeService;
import fr.uge.project.ruge.services.TransactionService;
import fr.uge.project.ruge.services.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Objects;

@Controller
@RequestMapping("/wallet")
public class WalletController {
  private final StripePaymentService stripePaymentService;
  private final TransactionService transactionService;
  private final UserService userService;
  private final SuperLikeService superLikeService;

  public WalletController(StripePaymentService stripePaymentService,
                          TransactionService transactionService,
                          UserService userService,
                          SuperLikeService superLikeService) {
    this.stripePaymentService = Objects.requireNonNull(stripePaymentService);
    this.transactionService = Objects.requireNonNull(transactionService);
    this.userService = Objects.requireNonNull(userService);
    this.superLikeService = Objects.requireNonNull(superLikeService);
  }

  @GetMapping
  public String walletHome(@RequestParam(name = "cancelled", required = false, defaultValue = "false") boolean cancelled,
                           @RequestParam(name = "connectReturn", required = false, defaultValue = "false") boolean connectReturn,
                           @RequestParam(name = "connectRefresh", required = false, defaultValue = "false") boolean connectRefresh,
                           Authentication authentication,
                           Model model) {
    var currentUser = CurrentUserSupport.currentUser(userService, authentication).orElse(null);
    if (currentUser == null) {
      return "redirect:/login";
    }

    model.addAttribute("cancelled", cancelled);
    model.addAttribute("connectReturn", connectReturn);
    model.addAttribute("connectRefresh", connectRefresh);
    model.addAttribute("walletBalance", stripePaymentService.getCurrentBalance(currentUser.getId()));
    var paymentLimits = stripePaymentService.getPaymentLimits();
    model.addAttribute("paymentMinAmount", paymentLimits.minAmount());
    model.addAttribute("paymentMaxAmount", paymentLimits.maxAmount());
    try {
      model.addAttribute("connectStatus", stripePaymentService.getConnectStatus(currentUser.getId()));
    } catch (IllegalArgumentException | IllegalStateException e) {
      model.addAttribute("connectStatus", new StripePaymentService.ConnectAccountStatus(false, false, null));
      model.addAttribute("connectErrorMessage", "Stripe bank account status is temporarily unavailable.");
    }
    model.addAttribute("redistributionDetails", transactionService.getRedistributionDetailsForUser(currentUser.getId(), 20));
    model.addAttribute("superLikeSent", superLikeService.getSuperLikesSentByUserId(currentUser.getId()));
    return "wallet/index";
  }

  @PostMapping("/checkout")
  public String createCheckout(@RequestParam("amount") String rawAmount,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
    var currentUser = CurrentUserSupport.currentUser(userService, authentication).orElse(null);
    if (currentUser == null) {
      return "redirect:/login";
    }

    try {
      var amount = parseAmount(rawAmount);
      var session = stripePaymentService.createCheckoutSession(currentUser.getId(), amount);
      return "redirect:" + session.checkoutUrl();
    } catch (IllegalArgumentException | IllegalStateException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
      return "redirect:/wallet";
    }
  }

  @GetMapping("/success")
  public String paymentSuccess(@RequestParam(name = "session_id", required = false) String sessionId,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
    var currentUser = CurrentUserSupport.currentUser(userService, authentication).orElse(null);
    if (currentUser == null) {
      return "redirect:/login";
    }

    try {
      var result = stripePaymentService.finalizeSuccessfulCheckout(currentUser.getId(), sessionId);
      if (result.outcome() == StripePaymentService.PaymentOutcome.CREDITED) {
        redirectAttributes.addFlashAttribute(
            "successMessage",
            "Payment confirmed. Your wallet has been credited with " + result.amount() + " EUR."
        );
      } else {
        redirectAttributes.addFlashAttribute("infoMessage", "This payment was already processed.");
      }
    } catch (IllegalArgumentException | IllegalStateException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/wallet";
  }

  @PostMapping("/connect/onboard")
  public String startConnectOnboarding(Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
    var currentUser = CurrentUserSupport.currentUser(userService, authentication).orElse(null);
    if (currentUser == null) {
      return "redirect:/login";
    }

    try {
      var connectInfo = stripePaymentService.createOrRefreshConnectOnboarding(currentUser.getId());
      return "redirect:" + connectInfo.onboardingUrl();
    } catch (IllegalArgumentException | IllegalStateException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
      return "redirect:/wallet";
    }
  }

  @PostMapping("/withdraw")
  public String withdrawToBank(@RequestParam("amount") String rawAmount,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
    var currentUser = CurrentUserSupport.currentUser(userService, authentication).orElse(null);
    if (currentUser == null) {
      return "redirect:/login";
    }

    try {
      var amount = parseAmount(rawAmount);
      var result = stripePaymentService.withdrawToBank(currentUser.getId(), amount);
      if (result.outcome() == StripePaymentService.WithdrawalOutcome.REQUESTED) {
        redirectAttributes.addFlashAttribute(
            "successMessage",
            "Withdrawal requested: " + result.amount() + " EUR. Stripe payout status: " + result.payoutStatus() + "."
        );
      } else {
        redirectAttributes.addFlashAttribute(
            "infoMessage",
            "Transfer completed to your Stripe connected account (" + result.amount() + " EUR). " +
                "Bank payout can be completed from Stripe if needed."
        );
      }
    } catch (IllegalArgumentException | IllegalStateException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }

    return "redirect:/wallet";
  }

  private static BigDecimal parseAmount(String rawAmount) {
    if (rawAmount == null || rawAmount.isBlank()) {
      throw new IllegalArgumentException("Amount is required.");
    }
    var normalized = rawAmount.trim().replace(",", ".");
    try {
      return new BigDecimal(normalized);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid amount.");
    }
  }
}
