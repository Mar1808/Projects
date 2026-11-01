package fr.uge.project.ruge.controllers.webClient;

import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.enums.Role;
import fr.uge.project.ruge.enums.Status;
import fr.uge.project.ruge.services.StripePaymentService;
import fr.uge.project.ruge.services.SuperLikeService;
import fr.uge.project.ruge.services.TransactionService;
import fr.uge.project.ruge.services.UserService;
import fr.uge.project.ruge.testsupport.MockitoTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest extends MockitoTestSupport {
  @Mock
  private StripePaymentService stripePaymentService;
  @Mock
  private TransactionService transactionService;
  @Mock
  private UserService userService;
  @Mock
  private SuperLikeService superLikeService;

  @Test
  void walletHomeRedirectsToLoginWhenUserIsAnonymous() {
    WalletController controller = controller();

    String view = controller.walletHome(false, false, false, null, new ExtendedModelMap());

    assertThat(view).isEqualTo("redirect:/login");
  }

  @Test
  void walletHomePopulatesModelWhenUserIsAuthenticated() {
    WalletController controller = controller();
    ExtendedModelMap model = new ExtendedModelMap();
    UserEntity user = user(5L, "user@ruge.test");
    var auth = new TestingAuthenticationToken("user@ruge.test", "pwd", "ROLE_MEMBER");

    when(userService.findByEmail("user@ruge.test")).thenReturn(Optional.of(user));
    when(stripePaymentService.getCurrentBalance(5L)).thenReturn(new BigDecimal("20.00"));
    when(stripePaymentService.getPaymentLimits())
        .thenReturn(new StripePaymentService.PaymentLimits(new BigDecimal("1.00"), new BigDecimal("5000.00")));
    when(stripePaymentService.getConnectStatus(5L))
        .thenReturn(new StripePaymentService.ConnectAccountStatus(false, false, null));
    when(transactionService.getRedistributionDetailsForUser(5L, 20)).thenReturn(List.of());

    String view = controller.walletHome(true, false, false, auth, model);

    assertThat(view).isEqualTo("wallet/index");
    assertThat(model.getAttribute("cancelled")).isEqualTo(true);
    assertThat(model.getAttribute("walletBalance")).isEqualTo(new BigDecimal("20.00"));
  }

  @Test
  void createCheckoutRedirectsToStripeSessionUrl() {
    WalletController controller = controller();
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    UserEntity user = user(7L, "user@ruge.test");
    var auth = new TestingAuthenticationToken("user@ruge.test", "pwd", "ROLE_MEMBER");
    when(userService.findByEmail("user@ruge.test")).thenReturn(Optional.of(user));
    when(stripePaymentService.createCheckoutSession(7L, new BigDecimal("12.50")))
        .thenReturn(new StripePaymentService.CheckoutSessionInfo("sess_1", "https://checkout.stripe.test/sess_1"));

    String view = controller.createCheckout("12.50", auth, redirect);

    assertThat(view).isEqualTo("redirect:https://checkout.stripe.test/sess_1");
  }

  @Test
  void createCheckoutReturnsWalletRedirectWhenAmountIsInvalid() {
    WalletController controller = controller();
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    UserEntity user = user(7L, "user@ruge.test");
    var auth = new TestingAuthenticationToken("user@ruge.test", "pwd", "ROLE_MEMBER");
    when(userService.findByEmail("user@ruge.test")).thenReturn(Optional.of(user));

    String view = controller.createCheckout("invalid", auth, redirect);

    assertThat(view).isEqualTo("redirect:/wallet");
    assertThat(redirect.getFlashAttributes().get("errorMessage")).isEqualTo("Invalid amount.");
  }

  @Test
  void paymentSuccessAddsSuccessMessageWhenCredited() {
    WalletController controller = controller();
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    UserEntity user = user(9L, "user@ruge.test");
    var auth = new TestingAuthenticationToken("user@ruge.test", "pwd", "ROLE_MEMBER");
    when(userService.findByEmail("user@ruge.test")).thenReturn(Optional.of(user));
    when(stripePaymentService.finalizeSuccessfulCheckout(9L, "session_1"))
        .thenReturn(new StripePaymentService.PaymentProcessResult(
            StripePaymentService.PaymentOutcome.CREDITED,
            new BigDecimal("15.00")
        ));

    String view = controller.paymentSuccess("session_1", auth, redirect);

    assertThat(view).isEqualTo("redirect:/wallet");
    assertThat(redirect.getFlashAttributes().get("successMessage"))
        .isEqualTo("Payment confirmed. Your wallet has been credited with 15.00 EUR.");
  }

  @Test
  void startConnectOnboardingRedirectsToStripeOnboardingUrl() {
    WalletController controller = controller();
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    UserEntity user = user(9L, "user@ruge.test");
    var auth = new TestingAuthenticationToken("user@ruge.test", "pwd", "ROLE_MEMBER");
    when(userService.findByEmail("user@ruge.test")).thenReturn(Optional.of(user));
    when(stripePaymentService.createOrRefreshConnectOnboarding(9L))
        .thenReturn(new StripePaymentService.ConnectOnboardingInfo("acct_1", "https://connect.stripe.test/acct_1"));

    String view = controller.startConnectOnboarding(auth, redirect);

    assertThat(view).isEqualTo("redirect:https://connect.stripe.test/acct_1");
  }

  @Test
  void withdrawToBankAddsSuccessMessage() {
    WalletController controller = controller();
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    UserEntity user = user(9L, "user@ruge.test");
    var auth = new TestingAuthenticationToken("user@ruge.test", "pwd", "ROLE_MEMBER");
    when(userService.findByEmail("user@ruge.test")).thenReturn(Optional.of(user));
    when(stripePaymentService.withdrawToBank(anyLong(), eq(new BigDecimal("12.50"))))
        .thenReturn(new StripePaymentService.WithdrawalResult(
            StripePaymentService.WithdrawalOutcome.REQUESTED,
            new BigDecimal("12.50"),
            "po_1",
            "tr_1",
            "pending",
            "Bank payout requested successfully."
        ));

    String view = controller.withdrawToBank("12.50", auth, redirect);

    assertThat(view).isEqualTo("redirect:/wallet");
    assertThat(redirect.getFlashAttributes().get("successMessage"))
        .isEqualTo("Withdrawal requested: 12.50 EUR. Stripe payout status: pending.");
  }

  @Test
  void withdrawToBankAddsInfoMessageWhenOnlyTransferSucceeded() {
    WalletController controller = controller();
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    UserEntity user = user(9L, "user@ruge.test");
    var auth = new TestingAuthenticationToken("user@ruge.test", "pwd", "ROLE_MEMBER");
    when(userService.findByEmail("user@ruge.test")).thenReturn(Optional.of(user));
    when(stripePaymentService.withdrawToBank(anyLong(), eq(new BigDecimal("12.50"))))
        .thenReturn(new StripePaymentService.WithdrawalResult(
            StripePaymentService.WithdrawalOutcome.TRANSFERRED_TO_CONNECTED_ACCOUNT,
            new BigDecimal("12.50"),
            null,
            "tr_1",
            "not_requested",
            "Funds moved to your Stripe connected account."
        ));

    String view = controller.withdrawToBank("12.50", auth, redirect);

    assertThat(view).isEqualTo("redirect:/wallet");
    assertThat(redirect.getFlashAttributes().get("infoMessage"))
        .isEqualTo("Transfer completed to your Stripe connected account (12.50 EUR). Bank payout can be completed from Stripe if needed.");
  }

  private WalletController controller() {
    return new WalletController(stripePaymentService, transactionService, userService, superLikeService);
  }

  private static UserEntity user(Long id, String email) {
    UserEntity user = new UserEntity();
    user.setId(id);
    user.setEmail(email);
    user.setPassword("pwd");
    user.setRole(Role.MEMBER);
    user.setStatus(Status.ACTIVE);
    return user;
  }
}
