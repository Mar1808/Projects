package fr.uge.project.ruge.dto;

public record AdminStatisticsDto(
    long totalUsers,
    long activeUsers,
    long bannedUsers,
    long adminUsers,
    long totalRecipes,
    long activeRecipes,
    long archivedRecipes,
    long totalReviews,
    long totalLikes,
    long totalSuperLikes,
    long totalUserFollows,
    long totalRecipeFollows,
    long totalTransactions,
    long walletTopupTransactions,
    long superLikeBenefitTransactions,
    long redistributionReserveTransactions,
    long monthlyRedistributionTransactions
) { }
