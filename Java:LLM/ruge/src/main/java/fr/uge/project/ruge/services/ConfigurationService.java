package fr.uge.project.ruge.services;

import fr.uge.project.ruge.entity.AppConfigurationEntity;
import fr.uge.project.ruge.repositories.AppConfigurationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ConfigurationService {
    private final AppConfigurationRepository repository;

    public ConfigurationService(AppConfigurationRepository repository) {
        this.repository = repository;
    }

    public AppConfigurationEntity getConfiguration() {
        return repository.findById(1)
                .orElseThrow(() -> new IllegalStateException("Configuration not found"));
    }

    @Transactional
    public void updatePrices(BigDecimal aiSearchPrice, BigDecimal superLikePrice) {
        var config = getConfiguration();
        config.setAiSearchPrice(aiSearchPrice);
        config.setSuperLikePrice(superLikePrice);
        repository.save(config);
    }

    public String getTermsContent() {
        return getConfiguration().getTermsContent();
    }

    @Transactional
    public void updateTermsContent(String termsContent) {
        var sanitizedContent = termsContent == null ? "" : termsContent.trim();
        if (sanitizedContent.isBlank()) {
            throw new IllegalArgumentException("Terms content cannot be empty.");
        }

        var config = getConfiguration();
        config.setTermsContent(sanitizedContent);
        repository.save(config);
    }
}
