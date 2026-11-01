package fr.uge.project.ruge.services;

import fr.uge.project.ruge.dto.RedistributionDetailDto;
import fr.uge.project.ruge.dto.TransactionDto;
import fr.uge.project.ruge.entity.TransactionEntity;
import fr.uge.project.ruge.enums.TransactionType;
import fr.uge.project.ruge.mapper.TransactionMapper;
import fr.uge.project.ruge.repositories.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    public TransactionService(TransactionRepository transactionRepository, TransactionMapper transactionMapper) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
        this.transactionMapper = Objects.requireNonNull(transactionMapper);
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getNLatestTransactions(int n) {
        return transactionRepository.findAllByOrderByDateDesc(PageRequest.of(0, n))
                .stream()
                .map(transactionMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getFilteredTransactions(LocalDateTime startDate, LocalDateTime endDate, Long walletSourceId, Long walletDestinationId, Pageable pageable) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("La date de début ne peut pas être après la date de fin.");
        }
        var startDateTime = (startDate != null)
                ? startDate
                : LocalDateTime.of(1900, 1, 1, 0, 0);

        var endDateTime = (endDate != null)
                ? endDate
                : LocalDateTime.of(2999, 12, 31, 23, 59, 59);

        var safeWalletSourceId = (walletSourceId != null) ? walletSourceId : -1L;
        var safeWalletDestinationId = (walletDestinationId != null) ? walletDestinationId : -1L;
        return transactionRepository.findWithFilters(startDateTime, endDateTime, safeWalletSourceId, safeWalletDestinationId, pageable)
                .stream()
                .map(transactionMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RedistributionDetailDto> getLatestRedistributionDetails(int n) {
        return transactionRepository.findByTransactionTypeOrderByDateDesc(
                        TransactionType.SUPERLIKE_MONTHLY_REDISTRIBUTION.name(),
                        PageRequest.of(0, n)
                )
                .stream()
                .map(this::toRedistributionDetail)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RedistributionDetailDto> getRedistributionDetailsForUser(Long userId, int n) {
        Objects.requireNonNull(userId);
        return transactionRepository.findByTransactionTypeAndTargetRecipe_Creator_IdOrderByDateDesc(
                        TransactionType.SUPERLIKE_MONTHLY_REDISTRIBUTION.name(),
                        userId,
                        PageRequest.of(0, n)
                )
                .stream()
                .map(this::toRedistributionDetail)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(transactionMapper::toDto)
                .toList();
    }

    private RedistributionDetailDto toRedistributionDetail(TransactionEntity tx) {
        var sourceRecipe = tx.getSourceRecipe();
        var beneficiaryRecipe = tx.getTargetRecipe();
        var beneficiaryUser = beneficiaryRecipe != null ? beneficiaryRecipe.getCreator() : null;

        return RedistributionDetailDto.builder()
                .transactionId(tx.getId())
                .date(tx.getDate())
                .amount(tx.getAmount())
                .sourceRecipeId(sourceRecipe != null ? sourceRecipe.getId() : null)
                .sourceRecipeName(sourceRecipe != null ? sourceRecipe.getName() : null)
                .beneficiaryRecipeId(beneficiaryRecipe != null ? beneficiaryRecipe.getId() : null)
                .beneficiaryRecipeName(beneficiaryRecipe != null ? beneficiaryRecipe.getName() : null)
                .beneficiaryUserId(beneficiaryUser != null ? beneficiaryUser.getId() : null)
                .beneficiaryUserName(beneficiaryUser != null ? beneficiaryUser.getName() : null)
                .build();
    }
}
