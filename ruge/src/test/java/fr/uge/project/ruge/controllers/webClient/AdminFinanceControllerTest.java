package fr.uge.project.ruge.controllers.webClient;

import fr.uge.project.ruge.dto.TransactionDto;
import fr.uge.project.ruge.entity.AppConfigurationEntity;
import fr.uge.project.ruge.services.ConfigurationService;
import fr.uge.project.ruge.services.SuperLikeService;
import fr.uge.project.ruge.services.TransactionService;
import fr.uge.project.ruge.services.WalletService;
import fr.uge.project.ruge.testsupport.MockitoTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Pageable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminFinanceControllerTest extends MockitoTestSupport {
  @Mock
  private WalletService walletService;
  @Mock
  private TransactionService transactionService;
  @Mock
  private ConfigurationService configurationService;
  @Mock
  private SuperLikeService superLikeService;

  @Test
  void indexPopulatesModelAndReturnsView() {
    AdminFinanceController controller = controller();
    ExtendedModelMap model = new ExtendedModelMap();
    AppConfigurationEntity config = config("0.99", "1.99");

    when(walletService.getDistributionAmount()).thenReturn(new BigDecimal("12.50"));
    when(walletService.getBenefitAmount()).thenReturn(new BigDecimal("8.75"));
    when(transactionService.getFilteredTransactions(any(), any(), any(), any(), any(Pageable.class))).thenReturn(List.of());
    when(transactionService.getLatestRedistributionDetails(20)).thenReturn(List.of());
    when(configurationService.getConfiguration()).thenReturn(config);

    String view = controller.index(null, null, null, null, 10, model);

    assertThat(view).isEqualTo("admin/manageFinances");
    assertThat(model.getAttribute("distributionAmount")).isEqualTo(new BigDecimal("12.50"));
    assertThat(model.getAttribute("benefitAmount")).isEqualTo(new BigDecimal("8.75"));
    assertThat(model.getAttribute("appConfig")).isEqualTo(config);
  }

  @Test
  void exportToCsvWritesHeaderAndRows() throws Exception {
    AdminFinanceController controller = controller();
    MockHttpServletResponse response = new MockHttpServletResponse();
    TransactionDto tx = TransactionDto.builder()
        .id(1L)
        .date(LocalDateTime.of(2026, 1, 2, 10, 0))
        .amount(new BigDecimal("4.20"))
        .description("Topup")
        .referenceId(77L)
        .transactionType("WALLET_TOPUP_STRIPE")
        .build();
    when(transactionService.getAllTransactions()).thenReturn(List.of(tx));

    controller.exportToCsv(response);

    assertThat(response.getContentType()).isEqualTo("text/csv; charset=UTF-8");
    assertThat(response.getContentAsString()).contains("ID;Date;Amount;SourceWallet;DestinationWallet");
    assertThat(response.getContentAsString()).contains("1;2026-01-02 10:00:00:000;4.20");
  }

  @Test
  void runRedistributionNowShowsNoPendingMessage() {
    AdminFinanceController controller = controller();
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    when(superLikeService.redistributeAllPendingByMonth())
        .thenReturn(new SuperLikeService.GlobalRedistributionResult(0, 0, 0, BigDecimal.ZERO));

    String view = controller.runRedistributionNow(redirect);

    assertThat(view).isEqualTo("redirect:/admin/finances");
    assertThat(redirect.getFlashAttributes().get("successMessage"))
        .isEqualTo("No pending super-likes to redistribute.");
  }

  private AdminFinanceController controller() {
    return new AdminFinanceController(walletService, transactionService, configurationService, superLikeService);
  }

  private static AppConfigurationEntity config(String ai, String superLike) {
    AppConfigurationEntity cfg = new AppConfigurationEntity();
    cfg.setAiSearchPrice(new BigDecimal(ai));
    cfg.setSuperLikePrice(new BigDecimal(superLike));
    cfg.setTermsContent("terms");
    return cfg;
  }
}
