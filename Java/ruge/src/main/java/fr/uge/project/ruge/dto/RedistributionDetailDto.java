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
public class RedistributionDetailDto {
  private Long transactionId;
  private LocalDateTime date;
  private BigDecimal amount;
  private Long sourceRecipeId;
  private String sourceRecipeName;
  private Long beneficiaryRecipeId;
  private String beneficiaryRecipeName;
  private Long beneficiaryUserId;
  private String beneficiaryUserName;
}
