package fr.uge.project.ruge.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.Event;
import com.stripe.model.Payout;
import com.stripe.model.Transfer;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.PayoutCreateParams;
import com.stripe.param.TransferCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import fr.uge.project.ruge.entity.TransactionEntity;
import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.entity.WalletEntity;
import fr.uge.project.ruge.enums.TransactionType;
import fr.uge.project.ruge.enums.WalletType;
import fr.uge.project.ruge.exceptions.UserNotFoundException;
import fr.uge.project.ruge.repositories.TransactionRepository;
import fr.uge.project.ruge.repositories.UserRepository;
import fr.uge.project.ruge.repositories.WalletRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;

@Service
public class StripePaymentService {
    private static final BigDecimal DEFAULT_MIN_AMOUNT = new BigDecimal("1.00");
    private static final BigDecimal DEFAULT_MAX_AMOUNT = new BigDecimal("5000.00");
    private static final String STRIPE_PROVIDER = "STRIPE";
    private static final String PAYMENT_MODE = "payment";
    private static final String PAYMENT_STATUS_PAID = "paid";
    private static final String SESSION_STATUS_COMPLETE = "complete";
    private static final String USER_ID_METADATA_KEY = "user_id";
    private static final String AMOUNT_CENTS_METADATA_KEY = "amount_cents";
    private static final String WALLET_CONNECT_RETURN_PATH = "/wallet?connectReturn=true";
    private static final String WALLET_CONNECT_REFRESH_PATH = "/wallet?connectRefresh=true";

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final ConfigurationService configurationService;
    private final ObjectMapper objectMapper;
    private final String stripeSecretKey;
    private final String stripeWebhookSecret;
    private final String stripeCurrency;
    private final String stripeConnectCountry;
    private final String appBaseUrl;

    public StripePaymentService(UserRepository userRepository,
                                WalletRepository walletRepository,
                                TransactionRepository transactionRepository,
                                ConfigurationService configurationService,
                                ObjectMapper objectMapper,
                                @Value("${stripe.secret-key:}") String stripeSecretKey,
                                @Value("${stripe.webhook-secret:}") String stripeWebhookSecret,
                                @Value("${stripe.currency:eur}") String stripeCurrency,
                                @Value("${stripe.connect-country:FR}") String stripeConnectCountry,
                                @Value("${app.base-url:http://localhost:8080}") String appBaseUrl) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.walletRepository = Objects.requireNonNull(walletRepository);
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
        this.configurationService = Objects.requireNonNull(configurationService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.stripeSecretKey = normalizeConfigValue(stripeSecretKey);
        this.stripeWebhookSecret = normalizeConfigValue(stripeWebhookSecret);
        this.stripeCurrency = normalizeCurrency(stripeCurrency);
        this.stripeConnectCountry = normalizeCountry(stripeConnectCountry);
        this.appBaseUrl = normalizeBaseUrl(appBaseUrl);
    }

    @Transactional(readOnly = true)
    public BigDecimal getCurrentBalance(Long userId) {
        Objects.requireNonNull(userId);
        var user = findUser(userId);
        var wallet = user.getWallet();
        if (wallet == null) {
            throw new IllegalStateException("User wallet not found.");
        }
        return wallet.getMoney();
    }

    @Transactional(readOnly = true)
    public PaymentLimits getPaymentLimits() {
        return resolvePaymentLimits();
    }

    @Transactional(readOnly = true)
    public CheckoutSessionInfo createCheckoutSession(Long userId, BigDecimal amount) {
        Objects.requireNonNull(userId);
        var normalizedAmount = validateAndNormalizeAmount(amount);
        var amountCents = toCents(normalizedAmount);

        ensureStripeConfigured();
        findUser(userId);
        var params = buildCheckoutSessionParams(userId, amountCents);

        try {
            var session = Session.create(params, requestOptions());
            if (session.getId() == null || session.getUrl() == null) {
                throw new IllegalStateException("Stripe returned an invalid session.");
            }
            return new CheckoutSessionInfo(session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw new IllegalStateException(resolveStripeMessage(e), e);
        }
    }

    @Transactional(readOnly = true)
    public ConnectAccountStatus getConnectStatus(Long userId) {
        Objects.requireNonNull(userId);
        var user = findUser(userId);
        var accountId = normalizeConfigValue(user.getStripeConnectAccountId());
        if (accountId.isBlank()) {
            return new ConnectAccountStatus(false, false, null);
        }
        if (stripeSecretKey.isBlank()) {
            return new ConnectAccountStatus(true, false, accountId);
        }

        var account = retrieveConnectAccount(accountId);
        var payoutsEnabled = Boolean.TRUE.equals(account.getPayoutsEnabled());
        var detailsSubmitted = Boolean.TRUE.equals(account.getDetailsSubmitted());
        return new ConnectAccountStatus(true, payoutsEnabled && detailsSubmitted, accountId);
    }

    @Transactional
    public ConnectOnboardingInfo createOrRefreshConnectOnboarding(Long userId) {
        Objects.requireNonNull(userId);
        ensureStripeConfigured();

        var user = findUser(userId);
        var connectAccountId = normalizeConfigValue(user.getStripeConnectAccountId());

        if (connectAccountId.isBlank()) {
            connectAccountId = createConnectAccount(user);
            user.setStripeConnectAccountId(connectAccountId);
            userRepository.save(user);
        }

        var onboardingUrl = createOnboardingUrl(connectAccountId);
        return new ConnectOnboardingInfo(connectAccountId, onboardingUrl);
    }

    @Transactional
    public WithdrawalResult withdrawToBank(Long userId, BigDecimal amount) {
        Objects.requireNonNull(userId);
        var normalizedAmount = validateAndNormalizeAmount(amount);
        var amountCents = toCents(normalizedAmount);
        ensureStripeConfigured();

        var user = findUser(userId);
        var userWallet = ensureUserWallet(user);
        ensureSufficientBalance(userWallet, normalizedAmount);

        var connectAccountId = requireValue(
                user.getStripeConnectAccountId(),
                "Connect your Stripe bank account before requesting a payout."
        );
        ensureConnectAccountReady(connectAccountId);
        var transferId = createTransferToConnectedAccount(connectAccountId, amountCents);
        var payoutAttempt = requestConnectedAccountPayout(connectAccountId, amountCents);
        var payoutWallet = moveFundsToPayoutWallet(userWallet, normalizedAmount);
        saveWithdrawalTransaction(userWallet, payoutWallet, normalizedAmount, transferId, payoutAttempt.payoutId());

        return new WithdrawalResult(
                payoutAttempt.outcome(),
                normalizedAmount,
                payoutAttempt.payoutId(),
                transferId,
                payoutAttempt.payoutStatus(),
                payoutAttempt.note()
        );
    }

    @Transactional
    public PaymentProcessResult finalizeSuccessfulCheckout(Long currentUserId, String sessionId) {
        Objects.requireNonNull(currentUserId);
        return processCompletedSession(retrieveSession(sessionId), currentUserId);
    }

    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Missing Stripe payload.");
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new IllegalArgumentException("Missing Stripe signature.");
        }
        ensureStripeWebhookConfigured();

        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, stripeWebhookSecret);
        } catch (SignatureVerificationException | RuntimeException e) {
            throw new IllegalArgumentException("Invalid Stripe webhook.", e);
        }

        if (!isHandledEvent(event.getType())) {
            return;
        }

        var session = extractSessionFromEvent(event);
        processCompletedSession(session, null);
    }

    PaymentProcessResult processCompletedSession(Session session, Long currentUserId) {
        var sessionData = validateAndExtractCompletedSessionData(session, currentUserId);
        if (transactionRepository.existsByProviderAndProviderPaymentId(STRIPE_PROVIDER, sessionData.sessionId())) {
            return new PaymentProcessResult(PaymentOutcome.ALREADY_PROCESSED, sessionData.amount());
        }

        var user = findUser(sessionData.paidUserId());
        var destinationWallet = ensureUserWallet(user);
        var sourceWallet = getOrCreateAdminDistributionWallet();
        saveTopUpTransaction(sourceWallet, destinationWallet, sessionData.amount(), sessionData.sessionId());
        creditWallet(destinationWallet, sessionData.amount());
        return new PaymentProcessResult(PaymentOutcome.CREDITED, sessionData.amount());
    }

    private Session retrieveSession(String sessionId) {
        var normalizedSessionId = requireValue(sessionId, "Missing Stripe session.");
        ensureStripeConfigured();
        try {
            return Session.retrieve(normalizedSessionId, requestOptions());
        } catch (StripeException e) {
            throw new IllegalStateException(resolveStripeMessage(e), e);
        }
    }

    private Account retrieveConnectAccount(String accountId) {
        var normalizedAccountId = requireValue(accountId, "Missing Stripe Connect account.");
        try {
            return Account.retrieve(normalizedAccountId, requestOptions());
        } catch (StripeException e) {
            throw new IllegalStateException(resolveStripeMessage(e), e);
        }
    }

    private String createConnectAccount(UserEntity user) {
        var params = AccountCreateParams.builder()
                .setType(AccountCreateParams.Type.EXPRESS)
                .setBusinessType(AccountCreateParams.BusinessType.INDIVIDUAL)
                .setCountry(stripeConnectCountry)
                .setEmail(user.getEmail())
                .build();

        try {
            var account = Account.create(params, requestOptions());
            return requireValue(account.getId(), "Stripe returned an invalid connected account.");
        } catch (StripeException e) {
            throw new IllegalStateException(resolveStripeMessage(e), e);
        }
    }

    private String createOnboardingUrl(String connectAccountId) {
        var params = AccountLinkCreateParams.builder()
                .setAccount(connectAccountId)
                .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                .setReturnUrl(appBaseUrl + WALLET_CONNECT_RETURN_PATH)
                .setRefreshUrl(appBaseUrl + WALLET_CONNECT_REFRESH_PATH)
                .build();

        try {
            var accountLink = AccountLink.create(params, requestOptions());
            return requireValue(accountLink.getUrl(), "Stripe returned an invalid onboarding url.");
        } catch (StripeException e) {
            throw new IllegalStateException(resolveStripeMessage(e), e);
        }
    }

    private String createTransferToConnectedAccount(String connectAccountId, long amountCents) {
        var params = TransferCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency(stripeCurrency)
                .setDestination(connectAccountId)
                .setDescription("Ruge wallet withdrawal transfer")
                .build();
        try {
            var transfer = Transfer.create(params, requestOptions());
            return requireValue(transfer.getId(), "Stripe returned an invalid transfer.");
        } catch (StripeException e) {
            throw new IllegalStateException(resolveStripeMessage(e), e);
        }
    }

    private Payout createConnectedAccountPayout(String connectAccountId, long amountCents) {
        var params = PayoutCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency(stripeCurrency)
                .setDescription("Ruge wallet withdrawal")
                .build();
        try {
            return Payout.create(params, connectedAccountRequestOptions(connectAccountId));
        } catch (StripeException e) {
            throw new IllegalStateException(resolveStripeMessage(e), e);
        }
    }

    private SessionCreateParams buildCheckoutSessionParams(Long userId, long amountCents) {
        return SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setClientReferenceId(String.valueOf(userId))
                .setSuccessUrl(appBaseUrl + "/wallet/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(appBaseUrl + "/wallet?cancelled=true")
                .putMetadata(USER_ID_METADATA_KEY, String.valueOf(userId))
                .putMetadata(AMOUNT_CENTS_METADATA_KEY, String.valueOf(amountCents))
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(stripeCurrency)
                                                .setUnitAmount(amountCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Ruge wallet top-up")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();
    }

    private CompletedSessionData validateAndExtractCompletedSessionData(Session session, Long currentUserId) {
        Objects.requireNonNull(session);
        var sessionId = requireValue(session.getId(), "Session Stripe invalide: id manquant.");
        validateSessionMode(session.getMode());
        validateSessionStatus(session.getStatus());
        validatePaymentStatus(session.getPaymentStatus());
        validateCurrency(session.getCurrency());

        var paidUserId = extractUserId(session);
        if (currentUserId != null && !currentUserId.equals(paidUserId)) {
            throw new IllegalArgumentException("This Stripe session does not match the signed-in user.");
        }

        var amountTotalCents = extractAmountTotalCents(session);
        validateAmountMetadata(session.getMetadata(), amountTotalCents);
        var amount = fromCents(amountTotalCents);
        return new CompletedSessionData(sessionId, paidUserId, amount);
    }

    private static void ensureSufficientBalance(WalletEntity userWallet, BigDecimal requestedAmount) {
        if (safeWalletBalance(userWallet).compareTo(requestedAmount) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance.");
        }
    }

    private void ensureConnectAccountReady(String connectAccountId) {
        var account = retrieveConnectAccount(connectAccountId);
        var payoutsEnabled = Boolean.TRUE.equals(account.getPayoutsEnabled());
        var detailsSubmitted = Boolean.TRUE.equals(account.getDetailsSubmitted());
        if (!payoutsEnabled || !detailsSubmitted) {
            throw new IllegalStateException("Stripe account is not ready for payouts yet.");
        }
    }

    private PayoutAttempt requestConnectedAccountPayout(String connectAccountId, long amountCents) {
        try {
            var payout = createConnectedAccountPayout(connectAccountId, amountCents);
            var payoutId = requireValue(payout.getId(), "Stripe returned an invalid payout.");
            var payoutStatus = normalizeConfigValue(payout.getStatus());
            return new PayoutAttempt(
                    WithdrawalOutcome.REQUESTED,
                    payoutId,
                    payoutStatus,
                    "Bank payout requested successfully."
            );
        } catch (IllegalStateException ignored) {
            return new PayoutAttempt(
                    WithdrawalOutcome.TRANSFERRED_TO_CONNECTED_ACCOUNT,
                    null,
                    "not_requested",
                    "Funds moved to your Stripe connected account."
            );
        }
    }

    private WalletEntity moveFundsToPayoutWallet(WalletEntity userWallet, BigDecimal amount) {
        var payoutWallet = getOrCreateAdminPayoutWallet();
        userWallet.setMoney(safeWalletBalance(userWallet).subtract(amount));
        payoutWallet.setMoney(safeWalletBalance(payoutWallet).add(amount));
        walletRepository.save(userWallet);
        walletRepository.save(payoutWallet);
        return payoutWallet;
    }

    private void saveWithdrawalTransaction(WalletEntity sourceWallet,
                                           WalletEntity destinationWallet,
                                           BigDecimal amount,
                                           String transferId,
                                           String payoutId) {
        var tx = new TransactionEntity();
        tx.setSourceWallet(sourceWallet);
        tx.setDestinationWallet(destinationWallet);
        tx.setAmount(amount);
        tx.setDescription(
                payoutId != null
                        ? "Stripe payout " + payoutId + " (transfer " + transferId + ")"
                        : "Stripe transfer " + transferId + " (payout not requested)"
        );
        tx.setDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        tx.setTransactionType(TransactionType.WALLET_WITHDRAWAL_STRIPE.name());
        tx.setProvider(STRIPE_PROVIDER);
        tx.setProviderPaymentId(payoutId != null ? payoutId : transferId);
        transactionRepository.save(tx);
    }

    private void saveTopUpTransaction(WalletEntity sourceWallet,
                                      WalletEntity destinationWallet,
                                      BigDecimal amount,
                                      String sessionId) {
        var tx = new TransactionEntity();
        tx.setSourceWallet(sourceWallet);
        tx.setDestinationWallet(destinationWallet);
        tx.setAmount(amount);
        tx.setDescription("Stripe top-up " + sessionId);
        tx.setDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        tx.setTransactionType(TransactionType.WALLET_TOPUP_STRIPE.name());
        tx.setProvider(STRIPE_PROVIDER);
        tx.setProviderPaymentId(sessionId);
        transactionRepository.save(tx);
    }

    private void creditWallet(WalletEntity wallet, BigDecimal amount) {
        wallet.setMoney(safeWalletBalance(wallet).add(amount));
        walletRepository.save(wallet);
    }

    private UserEntity findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }

    private static BigDecimal safeWalletBalance(WalletEntity wallet) {
        return wallet.getMoney() == null ? BigDecimal.ZERO : wallet.getMoney();
    }

    private Session extractSessionFromEvent(Event event) {
        var deserializer = event.getDataObjectDeserializer();
        var stripeObject = deserializer.getObject().orElse(null);
        return switch (stripeObject) {
            case Session session -> session;
            default -> {
                var rawJson = deserializer.getRawJson();
                if (rawJson == null || rawJson.isBlank()) {
                    throw new IllegalArgumentException("Invalid Stripe event: session not found.");
                }
                yield retrieveSession(extractSessionId(rawJson));
            }
        };
    }

    private String extractSessionId(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode idNode = root.get("id");
            if (idNode == null || idNode.asText().isBlank()) {
                throw new IllegalArgumentException("Invalid Stripe event: missing session id.");
            }
            return idNode.asText();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid Stripe event: unreadable session payload.", e);
        }
    }

    private WalletEntity ensureUserWallet(UserEntity user) {
        var destinationWallet = user.getWallet();
        if (destinationWallet != null) {
            return destinationWallet;
        }

        destinationWallet = new WalletEntity();
        destinationWallet.setType(WalletType.USER.name());
        destinationWallet.setMoney(BigDecimal.ZERO);
        user.setWallet(destinationWallet);
        userRepository.save(user);
        return destinationWallet;
    }

    private WalletEntity getOrCreateAdminDistributionWallet() {
        return getOrCreateWalletByType(WalletType.ADMIN_DISTRIBUTION.name());
    }

    private WalletEntity getOrCreateAdminPayoutWallet() {
        return getOrCreateWalletByType(WalletType.ADMIN_PAYOUT.name());
    }

    private WalletEntity getOrCreateWalletByType(String walletType) {
        return walletRepository.findFirstByType(walletType)
                .orElseGet(() -> {
                    var wallet = new WalletEntity();
                    wallet.setType(walletType);
                    wallet.setMoney(BigDecimal.ZERO);
                    return walletRepository.save(wallet);
                });
    }

    private RequestOptions requestOptions() {
        return RequestOptions.builder()
                .setApiKey(stripeSecretKey)
                .build();
    }

    private RequestOptions connectedAccountRequestOptions(String connectAccountId) {
        return RequestOptions.builder()
                .setApiKey(stripeSecretKey)
                .setStripeAccount(connectAccountId)
                .build();
    }

    private static void validateSessionMode(String mode) {
        if (mode != null && !PAYMENT_MODE.equalsIgnoreCase(mode)) {
            throw new IllegalArgumentException("Invalid Stripe session: unexpected payment mode.");
        }
    }

    private static void validateSessionStatus(String status) {
        if (status != null && !SESSION_STATUS_COMPLETE.equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Stripe session is not completed.");
        }
    }

    private static void validatePaymentStatus(String paymentStatus) {
        if (!PAYMENT_STATUS_PAID.equalsIgnoreCase(paymentStatus)) {
            throw new IllegalArgumentException("Stripe payment is not confirmed.");
        }
    }

    private void validateCurrency(String currency) {
        if (currency != null && !stripeCurrency.equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("Invalid Stripe session: unexpected currency.");
        }
    }

    private static Long extractUserId(Session session) {
        var metadata = session.getMetadata();
        var rawUserId = metadata == null ? null : metadata.get(USER_ID_METADATA_KEY);
        if (rawUserId == null || rawUserId.isBlank()) {
            rawUserId = session.getClientReferenceId();
        }
        if (rawUserId == null || rawUserId.isBlank()) {
            throw new IllegalArgumentException("Invalid Stripe session: missing user metadata.");
        }

        try {
            return Long.parseLong(rawUserId.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid Stripe session: invalid user metadata.");
        }
    }

    private static long extractAmountTotalCents(Session session) {
        var amountTotalCents = session.getAmountTotal();
        if (amountTotalCents == null || amountTotalCents <= 0L) {
            throw new IllegalArgumentException("Invalid Stripe amount.");
        }
        return amountTotalCents;
    }

    private static void validateAmountMetadata(Map<String, String> metadata, long amountTotalCents) {
        if (metadata == null) {
            return;
        }

        var expectedAmount = metadata.get(AMOUNT_CENTS_METADATA_KEY);
        if (expectedAmount == null || expectedAmount.isBlank()) {
            return;
        }

        long expectedAmountCents;
        try {
            expectedAmountCents = Long.parseLong(expectedAmount.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid Stripe session: invalid amount metadata.");
        }

        if (expectedAmountCents != amountTotalCents) {
            throw new IllegalArgumentException("Invalid Stripe session: amount mismatch.");
        }
    }

    private static boolean isHandledEvent(String eventType) {
        return "checkout.session.completed".equals(eventType)
                || "checkout.session.async_payment_succeeded".equals(eventType);
    }

    private static String resolveStripeMessage(StripeException e) {
        if (e.getStripeError() != null && e.getStripeError().getMessage() != null && !e.getStripeError().getMessage().isBlank()) {
            return e.getStripeError().getMessage();
        }
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            return e.getMessage();
        }
        return "Stripe error.";
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:8080";
        }
        var trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String normalizeConfigValue(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "eur";
        }
        return currency.trim().toLowerCase();
    }

    private static String normalizeCountry(String country) {
        if (country == null || country.isBlank()) {
            return "FR";
        }
        return country.trim().toUpperCase();
    }

    private static long toCents(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }

    private static BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }

    private PaymentLimits resolvePaymentLimits() {
        var configuration = configurationService.getConfiguration();
        var minAmount = normalizeConfiguredAmount(configuration.getStripeMinAmount(), DEFAULT_MIN_AMOUNT);
        var maxAmount = normalizeConfiguredAmount(configuration.getStripeMaxAmount(), DEFAULT_MAX_AMOUNT);
        if (minAmount.compareTo(maxAmount) > 0) {
            throw new IllegalStateException("Stripe amount configuration is invalid.");
        }
        return new PaymentLimits(minAmount, maxAmount);
    }

    private static BigDecimal normalizeConfiguredAmount(BigDecimal amount, BigDecimal fallback) {
        if (amount == null) {
            return fallback;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal validateAndNormalizeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required.");
        }

        var normalized = amount.setScale(2, RoundingMode.HALF_UP);
        var limits = resolvePaymentLimits();
        if (normalized.compareTo(limits.minAmount()) < 0) {
            throw new IllegalArgumentException("Minimum amount is " + formatAmount(limits.minAmount()) + ".");
        }
        if (normalized.compareTo(limits.maxAmount()) > 0) {
            throw new IllegalArgumentException("Maximum amount is " + formatAmount(limits.maxAmount()) + ".");
        }
        return normalized;
    }

    private void ensureStripeConfigured() {
        if (stripeSecretKey.isBlank()) {
            throw new IllegalStateException("STRIPE_SECRET_KEY is not configured.");
        }
    }

    private void ensureStripeWebhookConfigured() {
        ensureStripeConfigured();
        if (stripeWebhookSecret.isBlank()) {
            throw new IllegalStateException("STRIPE_WEBHOOK_SECRET is not configured.");
        }
    }

    private static String requireValue(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private record CompletedSessionData(String sessionId, Long paidUserId, BigDecimal amount) {
    }

    private record PayoutAttempt(WithdrawalOutcome outcome, String payoutId, String payoutStatus, String note) {
    }

    public record PaymentLimits(BigDecimal minAmount, BigDecimal maxAmount) {
    }

    public record CheckoutSessionInfo(String sessionId, String checkoutUrl) {
    }

    public record ConnectAccountStatus(boolean accountLinked, boolean bankAccountReady, String connectAccountId) {
    }

    public record ConnectOnboardingInfo(String connectAccountId, String onboardingUrl) {
    }

    public enum PaymentOutcome {
        CREDITED,
        ALREADY_PROCESSED
    }

    public record PaymentProcessResult(PaymentOutcome outcome, BigDecimal amount) {
    }

    public enum WithdrawalOutcome {
        REQUESTED,
        TRANSFERRED_TO_CONNECTED_ACCOUNT
    }

    public record WithdrawalResult(WithdrawalOutcome outcome,
                                   BigDecimal amount,
                                   String payoutId,
                                   String transferId,
                                   String payoutStatus,
                                   String note) {
    }
}
