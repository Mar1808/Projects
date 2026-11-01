package fr.uge.project.ruge.dto;

import java.util.List;

public record FinancesDashboardDto(
    Double totalRedistribution,
    Double totalBenefits,
    Double aiSearchPrice,
    Double superLikePrice,
    List<TransactionDto> transactions
) {
}
