package fr.uge.project.ruge.controllers.apiRestClient;

import fr.uge.project.ruge.services.StripePaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/stripe")
public class StripeWebhookController {
  private static final Logger LOG = LoggerFactory.getLogger(StripeWebhookController.class);

  private final StripePaymentService stripePaymentService;

  public StripeWebhookController(StripePaymentService stripePaymentService) {
    this.stripePaymentService = Objects.requireNonNull(stripePaymentService);
  }

  @PostMapping("/webhook")
  public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                              @RequestHeader(name = "Stripe-Signature", required = false) String signatureHeader) {
    try {
      stripePaymentService.handleWebhook(payload, signatureHeader);
      return ResponseEntity.ok("ok");
    } catch (IllegalArgumentException e) {
      LOG.warn("Stripe webhook rejected: {}", e.getMessage());
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (IllegalStateException e) {
      LOG.error("Stripe webhook failed: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }
  }
}
