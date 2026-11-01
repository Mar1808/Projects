package fr.uge.project.ruge.controllers.apiRestClient;

import fr.uge.project.ruge.services.StripePaymentService;
import fr.uge.project.ruge.services.SuperLikeService;
import fr.uge.project.ruge.services.TransactionService;
import fr.uge.project.ruge.services.UserService;
import fr.uge.project.ruge.services.rest.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users/wallet")
public class UserWalletApiRestController {

    private final StripePaymentService stripePaymentService;
    private final TransactionService transactionService;
    private final UserService userService;
    private final SuperLikeService superLikeService;
    private final JwtService jwtService;


    public UserWalletApiRestController(StripePaymentService stripePaymentService,
                                       TransactionService transactionService,
                                       UserService userService,
                                       SuperLikeService superLikeService,
                                       JwtService jwtService) {
        this.stripePaymentService = stripePaymentService;
        this.transactionService = transactionService;
        this.userService = userService;
        this.superLikeService = superLikeService;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getWalletInfo(
            @RequestHeader(value = "Authorization") String authorizationHeader) {
        log.info("Request for wallet info");

        var isAuthorized = jwtService.validateToken(authorizationHeader);
        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var email = jwtService.extractUsername(authorizationHeader);
        var userOpt = userService.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Long userId = userOpt.get().getId();
        Map<String, Object> response = new HashMap<>();

        // 1. Solde
        response.put("walletBalance", stripePaymentService.getCurrentBalance(userId));

        // 2. Limites Stripe
        var paymentLimits = stripePaymentService.getPaymentLimits();
        response.put("paymentMinAmount", paymentLimits.minAmount());
        response.put("paymentMaxAmount", paymentLimits.maxAmount());

        // 3. Statut de connexion Stripe
        try {
            var status = stripePaymentService.getConnectStatus(userId);
            response.put("connectStatus", status);
        } catch (IllegalArgumentException | IllegalStateException e) {
            response.put("connectStatus", null);
            response.put("connectErrorMessage", "Stripe bank account status is temporarily unavailable.");
        }

        // 4. Historique des redistributions (Mapping pour correspondre au front Kotlin)
        var rawRedistributions = transactionService.getRedistributionDetailsForUser(userId, 20);
        List<Map<String, Object>> safeRedistributions = rawRedistributions.stream().map(item -> {
            Map<String, Object> dtoMap = new HashMap<>();
            dtoMap.put("date", item.getDate() != null ? item.getDate().toString() : "");
            dtoMap.put("recetteSource", item.getSourceRecipeName() != null ? item.getSourceRecipeName() : "Inconnu");
            dtoMap.put("recetteBeneficiaire", item.getBeneficiaryRecipeName() != null ? item.getBeneficiaryRecipeName() : "Inconnu");
            dtoMap.put("amount", item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
            return dtoMap;
        }).toList();
        response.put("redistributionDetails", safeRedistributions);

        // 5. Super Likes (Correction du plantage no-session)
        // On récupère la liste des entités, mais on n'envoie QUE la taille (un Integer) dans la réponse JSON
        var superLikesList = superLikeService.getSuperLikesSentByUserId(userId);
        response.put("superLikeSent", superLikesList != null ? superLikesList.size() : 0);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> createCheckout(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @RequestParam("amount") String rawAmount) {

        var isAuthorized = jwtService.validateToken(authorizationHeader);
        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var email = jwtService.extractUsername(authorizationHeader);
        var userOpt = userService.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            var amount = parseAmount(rawAmount);
            var session = stripePaymentService.createCheckoutSession(userOpt.get().getId(), amount);
            return ResponseEntity.ok(Map.of("checkoutUrl", session.checkoutUrl()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, String>> withdrawToBank(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @RequestParam("amount") String rawAmount) {

        var isAuthorized = jwtService.validateToken(authorizationHeader);
        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var email = jwtService.extractUsername(authorizationHeader);
        var userOpt = userService.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            var amount = parseAmount(rawAmount);
            var result = stripePaymentService.withdrawToBank(userOpt.get().getId(), amount);

            if (result.outcome() == StripePaymentService.WithdrawalOutcome.REQUESTED) {
                return ResponseEntity.ok(Map.of("message", "Retrait demandé : " + result.amount() + " EUR."));
            } else {
                return ResponseEntity.ok(Map.of("message", "Transfert complété : " + result.amount() + " EUR."));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private BigDecimal parseAmount(String rawAmount) {
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