package fr.uge.project.ruge.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.checkout.Session;
import fr.uge.project.ruge.entity.TransactionEntity;
import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.entity.WalletEntity;
import fr.uge.project.ruge.enums.Role;
import fr.uge.project.ruge.enums.Status;
import fr.uge.project.ruge.enums.WalletType;
import fr.uge.project.ruge.repositories.TransactionRepository;
import fr.uge.project.ruge.repositories.UserRepository;
import fr.uge.project.ruge.repositories.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripePaymentServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private WalletRepository walletRepository;

  @Mock
  private TransactionRepository transactionRepository;

  @Mock
  private ConfigurationService configurationService;

  private StripePaymentService stripePaymentService;

  @BeforeEach
  void setUp() {
    stripePaymentService = new StripePaymentService(
        userRepository,
        walletRepository,
        transactionRepository,
        configurationService,
        new ObjectMapper(),
        "sk_test_123",
        "whsec_123",
        "eur",
        "FR",
        "http://localhost:8080"
    );
  }

  @Test
  void processCompletedSessionCreditsWalletOnce() {
    UserEntity user = createUserWithWallet(7L, "10.00");
    WalletEntity sourceWallet = createWallet(WalletType.ADMIN_DISTRIBUTION.name(), "0.00");
    Session session = createSession(Map.of("user_id", "7", "amount_cents", "1250"));

    when(transactionRepository.existsByProviderAndProviderPaymentId("STRIPE", "cs_test_123"))
        .thenReturn(false);
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    when(walletRepository.findFirstByType(WalletType.ADMIN_DISTRIBUTION.name()))
        .thenReturn(Optional.of(sourceWallet));
    when(transactionRepository.save(any(TransactionEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(walletRepository.save(any(WalletEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    StripePaymentService.PaymentProcessResult result = stripePaymentService.processCompletedSession(session, 7L);

    assertThat(result.outcome()).isEqualTo(StripePaymentService.PaymentOutcome.CREDITED);
    assertThat(result.amount()).isEqualByComparingTo("12.50");
    assertThat(user.getWallet().getMoney()).isEqualByComparingTo("22.50");

    ArgumentCaptor<TransactionEntity> txCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
    verify(transactionRepository).save(txCaptor.capture());
    TransactionEntity savedTransaction = txCaptor.getValue();
    assertThat(savedTransaction.getProvider()).isEqualTo("STRIPE");
    assertThat(savedTransaction.getProviderPaymentId()).isEqualTo("cs_test_123");
    assertThat(savedTransaction.getAmount()).isEqualByComparingTo("12.50");
  }

  @Test
  void processCompletedSessionSkipsAlreadyProcessedStripeSession() {
    Session session = createSession(Map.of("user_id", "7", "amount_cents", "1250"));
    when(transactionRepository.existsByProviderAndProviderPaymentId("STRIPE", "cs_test_123"))
        .thenReturn(true);

    StripePaymentService.PaymentProcessResult result = stripePaymentService.processCompletedSession(session, 7L);

    assertThat(result.outcome()).isEqualTo(StripePaymentService.PaymentOutcome.ALREADY_PROCESSED);
    assertThat(result.amount()).isEqualByComparingTo("12.50");
    verify(userRepository, never()).findById(anyLong());
    verify(transactionRepository, never()).save(any(TransactionEntity.class));
    verify(walletRepository, never()).save(any(WalletEntity.class));
  }

  @Test
  void processCompletedSessionRejectsMetadataAmountMismatch() {
    Session session = createSession(Map.of("user_id", "7", "amount_cents", "999"));

    assertThatThrownBy(() -> stripePaymentService.processCompletedSession(session, 7L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid Stripe session: amount mismatch.");
  }

  @Test
  void processCompletedSessionRejectsDifferentAuthenticatedUser() {
    Session session = createSession(Map.of("user_id", "7", "amount_cents", "1250"));

    assertThatThrownBy(() -> stripePaymentService.processCompletedSession(session, 8L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("This Stripe session does not match the signed-in user.");
  }

  private static Session createSession(Map<String, String> metadata) {
    Session session = new Session();
    session.setId("cs_test_123");
    session.setMode("payment");
    session.setStatus("complete");
    session.setPaymentStatus("paid");
    session.setCurrency("eur");
    session.setAmountTotal(1250L);
    session.setMetadata(metadata);
    session.setClientReferenceId("7");
    return session;
  }

  private static UserEntity createUserWithWallet(Long userId, String amount) {
    UserEntity user = new UserEntity();
    user.setId(userId);
    user.setEmail("user@example.com");
    user.setName("User");
    user.setPassword("secret");
    user.setRole(Role.MEMBER);
    user.setStatus(Status.ACTIVE);
    user.setWallet(createWallet(WalletType.USER.name(), amount));
    return user;
  }

  private static WalletEntity createWallet(String type, String amount) {
    WalletEntity wallet = new WalletEntity();
    wallet.setType(type);
    wallet.setMoney(new BigDecimal(amount));
    return wallet;
  }
}
