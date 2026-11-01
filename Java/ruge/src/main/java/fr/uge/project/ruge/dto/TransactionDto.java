package fr.uge.project.ruge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
  private Long id;
  private Long sourceUserId;
  private Long targetUserId;
  private BigDecimal amount;
  private String transactionType;
  private String description;
  private LocalDateTime date;
  private Long referenceId;
  private Long sourceRecipeId;
  private Long targetRecipeId;
}
