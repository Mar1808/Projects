package fr.uge.project.ruge.controllers.apiRestClient;

import fr.uge.project.ruge.services.StripePaymentService;
import fr.uge.project.ruge.testsupport.MockitoTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StripeWebhookControllerTest extends MockitoTestSupport {
  @Mock
  private StripePaymentService stripePaymentService;

  @Test
  void handleWebhookReturnsOkOnSuccess() {
    StripeWebhookController controller = new StripeWebhookController(stripePaymentService);

    var response = controller.handleWebhook("payload", "sig");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("ok");
    verify(stripePaymentService).handleWebhook("payload", "sig");
  }

  @Test
  void handleWebhookReturnsBadRequestOnIllegalArgument() {
    StripeWebhookController controller = new StripeWebhookController(stripePaymentService);
    doThrow(new IllegalArgumentException("Invalid signature")).when(stripePaymentService).handleWebhook("payload", "sig");

    var response = controller.handleWebhook("payload", "sig");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isEqualTo("Invalid signature");
  }

  @Test
  void handleWebhookReturnsServerErrorOnIllegalState() {
    StripeWebhookController controller = new StripeWebhookController(stripePaymentService);
    doThrow(new IllegalStateException("Stripe down")).when(stripePaymentService).handleWebhook("payload", "sig");

    var response = controller.handleWebhook("payload", "sig");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isEqualTo("Stripe down");
  }
}
