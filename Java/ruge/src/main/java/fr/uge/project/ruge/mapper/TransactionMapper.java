package fr.uge.project.ruge.mapper;

import fr.uge.project.ruge.dto.TransactionDto;
import fr.uge.project.ruge.entity.TransactionEntity;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {
  public TransactionDto toDto(TransactionEntity entity) {
    if (entity == null) {
      return null;
    }
    return TransactionDto.builder()
            .id(entity.getId())
            .sourceUserId(entity.getSourceWallet() != null ? entity.getSourceWallet().getId() : null)
            .targetUserId(entity.getDestinationWallet() != null ? entity.getDestinationWallet().getId() : null)
            .amount(entity.getAmount())
            .transactionType(entity.getTransactionType())
            .description(entity.getDescription())
            .date(entity.getDate())
            .referenceId(entity.getReferenceId())
            .sourceRecipeId(entity.getSourceRecipe() != null ? entity.getSourceRecipe().getId() : null)
            .targetRecipeId(entity.getTargetRecipe() != null ? entity.getTargetRecipe().getId() : null)
            .build();
  }
}
