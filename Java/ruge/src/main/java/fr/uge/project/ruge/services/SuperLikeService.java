package fr.uge.project.ruge.services;

import fr.uge.project.ruge.entity.*;
import fr.uge.project.ruge.enums.TransactionType;
import fr.uge.project.ruge.enums.WalletType;
import fr.uge.project.ruge.exceptions.RecipeNotFoundException;
import fr.uge.project.ruge.exceptions.UserNotFoundException;
import fr.uge.project.ruge.factory.SuperLikeStrategyFactory;
import fr.uge.project.ruge.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SuperLikeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuperLikeService.class);
    private static final BigDecimal DISTRIBUTION_SHARE_RATE = new BigDecimal("0.60");
    private static final String DESCRIPTION_SITE_BENEFIT = "Super-like recette #%d - benefice site (%s)";
    private static final String DESCRIPTION_DISTRIBUTION_RESERVE = "Super-like recette #%d - reserve redistribution (%s)";
    private static final String DESCRIPTION_MONTHLY_REDISTRIBUTION =
            "Redistribution %s - recette source #%d -> recette beneficiaire #%d";

    private final SuperLikeRepository superLikeRepository;
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final SuperLikeStrategyFactory strategyFactory;

    public record MonthlyRedistributionResult(
            YearMonth month,
            int processedSuperLikeCount,
            int createdTransactionCount,
            BigDecimal totalRedistributed
    ) {
    }

    public record GlobalRedistributionResult(
            int processedMonthCount,
            int processedSuperLikeCount,
            int createdTransactionCount,
            BigDecimal totalRedistributed
    ) {
    }

    public SuperLikeService(SuperLikeRepository superLikeRepository,
                            UserRepository userRepository,
                            RecipeRepository recipeRepository,
                            WalletRepository walletRepository,
                            TransactionRepository transactionRepository,
                            SuperLikeStrategyFactory strategyFactory) {
        this.superLikeRepository = Objects.requireNonNull(superLikeRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.recipeRepository = Objects.requireNonNull(recipeRepository);
        this.walletRepository = Objects.requireNonNull(walletRepository);
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
        this.strategyFactory = Objects.requireNonNull(strategyFactory);
    }

    @Transactional
    public void superLike(Long senderId, Long recipeId) {
        if (senderId == null || recipeId == null) {
            throw new IllegalArgumentException("Utilisateur ou recette invalide.");
        }

        var sender = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouve: " + senderId));
        var recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException("Recette non trouvee: " + recipeId));

        var receiver = recipe.getCreator();
        if (receiver == null) {
            throw new IllegalArgumentException("Impossible de super-liker une recette sans auteur.");
        }
        if (Objects.equals(receiver.getId(), senderId)) {
            throw new IllegalArgumentException("Vous ne pouvez pas super-liker votre propre recette.");
        }

        var senderWallet = requireUserWallet(sender);

        var strategy = strategyFactory.getStrategy();
        var impact = strategy.calculateImpact(sender, 1);

        if (senderWallet.getMoney().compareTo(impact.priceToPay()) < 0) {
            throw new IllegalArgumentException("Solde insuffisant. Requis : " + impact.priceToPay());
        }

        var adminBenefWallet = getOrCreateWallet(WalletType.ADMIN_BENEF);
        var adminDistributionWallet = getOrCreateWallet(WalletType.ADMIN_DISTRIBUTION);
        ensureAdminBenefitCoverage(adminBenefWallet, impact.siteBenefShare());

        senderWallet.setMoney(senderWallet.getMoney().subtract(impact.priceToPay()));
        adminBenefWallet.setMoney(adminBenefWallet.getMoney().add(impact.siteBenefShare()));
        adminDistributionWallet.setMoney(adminDistributionWallet.getMoney().add(impact.distributionShare()));

        walletRepository.save(senderWallet);
        walletRepository.save(adminBenefWallet);
        walletRepository.save(adminDistributionWallet);

        var superLike = new SuperLikeEntity();
        superLike.setSender(sender);
        superLike.setReceiver(receiver);
        superLike.setRecipe(recipe);
        superLike.setAmount(impact.priceToPay());
        superLike.setIsRedistributed(false);
        superLike.setDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        superLike.setPromotion(impact.promotion());
        superLike = superLikeRepository.save(superLike);

        transactionRepository.save(createTransaction(
                senderWallet, adminBenefWallet, impact.siteBenefShare(),
                String.format(DESCRIPTION_SITE_BENEFIT, recipeId, impact.explanation()),
                recipe, recipe, superLike.getId(), TransactionType.SUPERLIKE_SITE_BENEFIT
        ));

        transactionRepository.save(createTransaction(
                senderWallet, adminDistributionWallet, impact.distributionShare(),
                String.format(DESCRIPTION_DISTRIBUTION_RESERVE, recipeId, impact.explanation()),
                recipe, recipe, superLike.getId(), TransactionType.SUPERLIKE_REDISTRIBUTION_RESERVE
        ));
    }

    @Transactional(readOnly = true)
    public boolean hasSuperLiked(Long userId, Long recipeId) {
        if (userId == null || recipeId == null) {
            return false;
        }
        return superLikeRepository.existsBySender_IdAndRecipe_Id(userId, recipeId);
    }

    @Transactional(readOnly = true)
    public long countSuperLikes(Long recipeId) {
        if (recipeId == null) {
            return 0L;
        }
        return superLikeRepository.countByRecipe_Id(recipeId);
    }

    @Transactional
    public MonthlyRedistributionResult redistributePreviousMonth() {
        return redistributeForMonth(YearMonth.now().minusMonths(1));
    }

    @Transactional
    public GlobalRedistributionResult redistributeAllPendingByMonth() {
        var pendingSuperLikes = superLikeRepository.findAllPending();
        if (pendingSuperLikes.isEmpty()) {
            return new GlobalRedistributionResult(0, 0, 0, BigDecimal.ZERO);
        }

        var months = pendingSuperLikes.stream()
                .map(sl -> YearMonth.from(sl.getDate()))
                .distinct()
                .sorted()
                .toList();

        int totalProcessedSuperLikes = 0;
        int totalCreatedTransactions = 0;
        var totalRedistributed = BigDecimal.ZERO;

        for (var month : months) {
            var result = redistributeForMonth(month);
            totalProcessedSuperLikes += result.processedSuperLikeCount();
            totalCreatedTransactions += result.createdTransactionCount();
            totalRedistributed = totalRedistributed.add(result.totalRedistributed());
        }

        return new GlobalRedistributionResult(
                months.size(), totalProcessedSuperLikes, totalCreatedTransactions, totalRedistributed
        );
    }

    @Transactional
    public MonthlyRedistributionResult redistributeForMonth(YearMonth month) {
        Objects.requireNonNull(month);

        var periodStart = month.atDay(1).atStartOfDay();
        var periodEnd = month.plusMonths(1).atDay(1).atStartOfDay();
        var pendingSuperLikes = superLikeRepository.findAllPendingForPeriod(periodStart, periodEnd);

        if (pendingSuperLikes.isEmpty()) {
            return new MonthlyRedistributionResult(month, 0, 0, BigDecimal.ZERO);
        }

        var distributionWallet = getOrCreateWallet(WalletType.ADMIN_DISTRIBUTION);
        var superLikesByRecipe = groupSuperLikesByRecipe(pendingSuperLikes);
        var reservedAmountsBySuperLikeId = loadReservedAmountsInCents(pendingSuperLikes);

        long totalRedistributedCents = 0L;
        var redistributionTransactions = new ArrayList<TransactionEntity>();
        var creditsByWalletId = new HashMap<Long, Long>();
        var walletById = new HashMap<Long, WalletEntity>();

        for (var entry : superLikesByRecipe.entrySet()) {
            var sourceRecipe = entry.getValue().getFirst().getRecipe();
            long totalCentsForRecipe = computeReservedAmountInCents(entry.getValue(), reservedAmountsBySuperLikeId);
            if (totalCentsForRecipe <= 0) continue;

            var chain = resolveVariantChain(sourceRecipe);
            var shares = splitAcrossVariantChain(totalCentsForRecipe, chain);

            for (var shareEntry : shares.entrySet()) {
                var beneficiaryRecipe = shareEntry.getKey();
                var shareCents = shareEntry.getValue();
                if (shareCents <= 0) continue;

                var beneficiaryWallet = requireRecipeCreatorWallet(beneficiaryRecipe);
                creditsByWalletId.merge(beneficiaryWallet.getId(), shareCents, Long::sum);
                walletById.putIfAbsent(beneficiaryWallet.getId(), beneficiaryWallet);

                redistributionTransactions.add(createTransaction(
                        distributionWallet, beneficiaryWallet, fromCents(shareCents),
                        String.format(DESCRIPTION_MONTHLY_REDISTRIBUTION, month, sourceRecipe.getId(), beneficiaryRecipe.getId()),
                        sourceRecipe, beneficiaryRecipe, sourceRecipe.getId(), TransactionType.SUPERLIKE_MONTHLY_REDISTRIBUTION
                ));
                totalRedistributedCents += shareCents;
            }
        }

        if (totalRedistributedCents > 0) {
            var totalRedistributedAmount = fromCents(totalRedistributedCents);
            if (distributionWallet.getMoney().compareTo(totalRedistributedAmount) < 0) {
                throw new IllegalStateException("Solde insuffisant sur le wallet de redistribution.");
            }

            distributionWallet.setMoney(distributionWallet.getMoney().subtract(totalRedistributedAmount));
            walletRepository.save(distributionWallet);

            for (var credit : creditsByWalletId.entrySet()) {
                var wallet = walletById.get(credit.getKey());
                wallet.setMoney(wallet.getMoney().add(fromCents(credit.getValue())));
                walletRepository.save(wallet);
            }
            transactionRepository.saveAll(redistributionTransactions);
        }

        pendingSuperLikes.forEach(sl -> sl.setIsRedistributed(true));
        superLikeRepository.saveAll(pendingSuperLikes);

        return new MonthlyRedistributionResult(
                month, pendingSuperLikes.size(), redistributionTransactions.size(), fromCents(totalRedistributedCents)
        );
    }

    private WalletEntity requireUserWallet(UserEntity user) {
        var wallet = user.getWallet();
        if (wallet == null) throw new IllegalStateException("Le wallet utilisateur est introuvable.");
        if (wallet.getMoney() == null) wallet.setMoney(BigDecimal.ZERO);
        return wallet;
    }

    private WalletEntity getOrCreateWallet(WalletType type) {
        return walletRepository.findFirstByType(type.name())
                .map(w -> {
                    if (w.getMoney() == null) w.setMoney(BigDecimal.ZERO);
                    return w;
                })
                .orElseGet(() -> {
                    WalletEntity w = new WalletEntity();
                    w.setType(type.name());
                    w.setMoney(BigDecimal.ZERO);
                    return walletRepository.save(w);
                });
    }

    private WalletEntity requireRecipeCreatorWallet(RecipeEntity recipe) {
        var creator = recipe.getCreator();
        if (creator == null) throw new IllegalStateException("La recette #" + recipe.getId() + " n'a pas d'auteur.");
        return requireUserWallet(creator);
    }

    private static BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static void ensureAdminBenefitCoverage(WalletEntity adminBenefWallet, BigDecimal siteBenefShare) {
        if (siteBenefShare == null || siteBenefShare.signum() >= 0) {
            return;
        }
        var requiredAmount = siteBenefShare.negate();
        if (adminBenefWallet.getMoney().compareTo(requiredAmount) < 0) {
            throw new IllegalStateException("Solde insuffisant sur le wallet de bénéfice pour financer un super-like gratuit.");
        }
    }

    private static BigDecimal calculateDistributionShare(BigDecimal amount) {
        return amount.multiply(DISTRIBUTION_SHARE_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    private static long computeRedistributionAmountInCents(List<SuperLikeEntity> superLikes) {
        return superLikes.stream()
                .map(SuperLikeEntity::getAmount)
                .map(SuperLikeService::normalizeAmount)
                .map(SuperLikeService::calculateDistributionShare)
                .mapToLong(SuperLikeService::toCents)
                .sum();
    }

    private Map<Long, Long> loadReservedAmountsInCents(List<SuperLikeEntity> superLikes) {
        var superLikeIds = superLikes.stream().map(SuperLikeEntity::getId).filter(Objects::nonNull).toList();
        var reserveTransactions = transactionRepository.findReserveTransactionsForSuperLikeIds(
                superLikeIds, WalletType.ADMIN_DISTRIBUTION.name(), WalletType.USER.name(), TransactionType.SUPERLIKE_REDISTRIBUTION_RESERVE.name()
        );
        return reserveTransactions.stream().filter(tx -> tx.getReferenceId() != null && tx.getAmount() != null)
                .collect(Collectors.toMap(TransactionEntity::getReferenceId, tx -> toCents(normalizeAmount(tx.getAmount())), Long::sum));
    }

    private long computeReservedAmountInCents(List<SuperLikeEntity> superLikes, Map<Long, Long> reservedAmounts) {
        long total = 0L;
        for (var sl : superLikes) {
            if (sl.getId() != null && reservedAmounts.containsKey(sl.getId())) {
                total += reservedAmounts.get(sl.getId());
            } else {
                total += computeRedistributionAmountInCents(List.of(sl));
            }
        }
        return total;
    }

    private static Map<Long, List<SuperLikeEntity>> groupSuperLikesByRecipe(List<SuperLikeEntity> superLikes) {
        var grouped = new LinkedHashMap<Long, List<SuperLikeEntity>>();
        for (var sl : superLikes) {
            grouped.computeIfAbsent(sl.getRecipe().getId(), ignored -> new ArrayList<>()).add(sl);
        }
        return grouped;
    }

    private List<RecipeEntity> resolveVariantChain(RecipeEntity sourceRecipe) {
        var chain = new ArrayList<RecipeEntity>();
        var visited = new HashSet<Long>();
        RecipeEntity current = sourceRecipe;
        while (current != null) {
            if (current.getId() != null && !visited.add(current.getId())) {
                throw new IllegalStateException("Cycle detecte : recette #" + sourceRecipe.getId());
            }
            chain.add(current);
            current = current.getVariant();
        }
        return chain;
    }

    private static Map<RecipeEntity, Long> splitAcrossVariantChain(long totalCents, List<RecipeEntity> chain) {
        var shares = new LinkedHashMap<RecipeEntity, Long>();
        if (chain.isEmpty() || totalCents <= 0) return shares;

        long remaining = totalCents;
        for (int i = 0; i < chain.size() - 1; i++) {
            long share = ceilHalf(remaining);
            shares.merge(chain.get(i), share, Long::sum);
            remaining -= share;
        }
        shares.merge(chain.getLast(), remaining, Long::sum);
        return shares;
    }

    private static long ceilHalf(long value) {
        return (value + 1) / 2;
    }

    private static long toCents(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private static BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }

    private static TransactionEntity createTransaction(WalletEntity sourceWallet, WalletEntity destinationWallet,
                                                       BigDecimal amount, String description, RecipeEntity sourceRecipe,
                                                       RecipeEntity targetRecipe, Long referenceId, TransactionType transactionType) {
        TransactionEntity tx = new TransactionEntity();
        tx.setSourceWallet(sourceWallet);
        tx.setDestinationWallet(destinationWallet);
        tx.setAmount(amount);
        tx.setDescription(description);
        tx.setSourceRecipe(sourceRecipe);
        tx.setTargetRecipe(targetRecipe);
        tx.setReferenceId(referenceId);
        tx.setTransactionType(transactionType.name());
        tx.setDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        return tx;
    }

    public List<SuperLikeEntity> getSuperLikesSentByUserId(Long userId) {
        return superLikeRepository.findBySender_Id(userId);
    }
}
