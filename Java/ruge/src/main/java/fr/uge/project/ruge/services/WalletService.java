package fr.uge.project.ruge.services;

import fr.uge.project.ruge.entity.TransactionEntity;
import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.entity.WalletEntity;
import fr.uge.project.ruge.enums.WalletType;
import fr.uge.project.ruge.factory.AiSearchStrategyFactory;
import fr.uge.project.ruge.repositories.TransactionRepository;
import fr.uge.project.ruge.repositories.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final AiSearchStrategyFactory strategyFactory;
    private final ConfigurationService configurationService;

    public WalletService(WalletRepository walletRepository,
                         TransactionRepository transactionRepository,
                         AiSearchStrategyFactory strategyFactory,
                         ConfigurationService configurationService) {
        this.walletRepository = Objects.requireNonNull(walletRepository);
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
        this.strategyFactory = Objects.requireNonNull(strategyFactory);
        this.configurationService = Objects.requireNonNull(configurationService);
    }

    @Transactional
    public boolean payForSearchAi(UserEntity user) {
        Objects.requireNonNull(user);
        var userWallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Le wallet utilisateur est introuvable."));

        var strategy = strategyFactory.getStrategy();
        var impact = strategy.calculateImpact(user);
        var priceToPay = impact.priceToPay();

        if (priceToPay.compareTo(BigDecimal.ZERO) == 0) {
            return true;
        }

        if (userWallet.getMoney().compareTo(priceToPay) < 0) {
            System.out.println("Solde insuffisant. Requis : " + priceToPay + ", Actuel : " + userWallet.getMoney());
            return false;
        }

        var adminBenefWallet = getOrCreateWallet(WalletType.ADMIN_BENEF);

        userWallet.setMoney(userWallet.getMoney().subtract(priceToPay));
        adminBenefWallet.setMoney(adminBenefWallet.getMoney().add(priceToPay));

        walletRepository.save(userWallet);
        walletRepository.save(adminBenefWallet);

        TransactionEntity transaction = new TransactionEntity();
        transaction.setSourceWallet(userWallet);
        transaction.setDestinationWallet(adminBenefWallet);
        transaction.setAmount(priceToPay);
        transaction.setDescription("Paiement Recherche IA (" + impact.explanation() + ")");
        transaction.setDate(LocalDateTime.now());
        transactionRepository.save(transaction);

        return true;
    }

    public BigDecimal getAiSearchPrice(UserEntity user) {
        if (user == null) {
            return configurationService.getConfiguration().getAiSearchPrice();
        }
        return strategyFactory.getStrategy().calculateImpact(user).priceToPay();
    }

    public BigDecimal getDistributionAmount() {
        var wallet = walletRepository.findFirstByType(WalletType.ADMIN_DISTRIBUTION.name())
                .orElseThrow(() -> new IllegalStateException("Le wallet de distribution est introuvable."));
        return wallet.getMoney();
    }

    public BigDecimal getBenefitAmount() {
        var wallet = walletRepository.findFirstByType(WalletType.ADMIN_BENEF.name())
                .orElseThrow(() -> new IllegalStateException("Le wallet de bénéfice est introuvable."));
        return wallet.getMoney();
    }

    private WalletEntity getOrCreateWallet(WalletType type) {
        return walletRepository.findFirstByType(type.name())
                .orElseGet(() -> {
                    WalletEntity wallet = new WalletEntity();
                    wallet.setType(type.name());
                    wallet.setMoney(BigDecimal.ZERO);
                    return walletRepository.save(wallet);
                });
    }
}