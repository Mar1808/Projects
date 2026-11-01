package fr.uge.project.ruge.services;

import fr.uge.project.ruge.dto.AdminStatisticsDto;
import fr.uge.project.ruge.enums.Role;
import fr.uge.project.ruge.enums.Status;
import fr.uge.project.ruge.enums.TransactionType;
import fr.uge.project.ruge.repositories.FollowRecipeRepository;
import fr.uge.project.ruge.repositories.FollowRepository;
import fr.uge.project.ruge.repositories.LikerRepository;
import fr.uge.project.ruge.repositories.RecipeRepository;
import fr.uge.project.ruge.repositories.RecipeReviewRepository;
import fr.uge.project.ruge.repositories.SuperLikeRepository;
import fr.uge.project.ruge.repositories.TransactionRepository;
import fr.uge.project.ruge.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class AdminStatisticsService {
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeReviewRepository recipeReviewRepository;
    private final LikerRepository likerRepository;
    private final SuperLikeRepository superLikeRepository;
    private final FollowRepository followRepository;
    private final FollowRecipeRepository followRecipeRepository;
    private final TransactionRepository transactionRepository;

    public AdminStatisticsService(UserRepository userRepository,
                                  RecipeRepository recipeRepository,
                                  RecipeReviewRepository recipeReviewRepository,
                                  LikerRepository likerRepository,
                                  SuperLikeRepository superLikeRepository,
                                  FollowRepository followRepository,
                                  FollowRecipeRepository followRecipeRepository,
                                  TransactionRepository transactionRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.recipeRepository = Objects.requireNonNull(recipeRepository);
        this.recipeReviewRepository = Objects.requireNonNull(recipeReviewRepository);
        this.likerRepository = Objects.requireNonNull(likerRepository);
        this.superLikeRepository = Objects.requireNonNull(superLikeRepository);
        this.followRepository = Objects.requireNonNull(followRepository);
        this.followRecipeRepository = Objects.requireNonNull(followRecipeRepository);
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
    }

    @Transactional(readOnly = true)
    public AdminStatisticsDto getStatistics() {
        return new AdminStatisticsDto(
                userRepository.count(),
                userRepository.countByStatus(Status.ACTIVE),
                userRepository.countByStatus(Status.BANNED),
                userRepository.countByRole(Role.ADMIN),
                recipeRepository.count(),
                recipeRepository.countByActiveTrue(),
                recipeRepository.countByActiveFalse(),
                recipeReviewRepository.count(),
                likerRepository.count(),
                superLikeRepository.count(),
                followRepository.count(),
                followRecipeRepository.count(),
                transactionRepository.count(),
                transactionRepository.countByTransactionType(TransactionType.WALLET_TOPUP_STRIPE.name()),
                transactionRepository.countByTransactionType(TransactionType.SUPERLIKE_SITE_BENEFIT.name()),
                transactionRepository.countByTransactionType(TransactionType.SUPERLIKE_REDISTRIBUTION_RESERVE.name()),
                transactionRepository.countByTransactionType(TransactionType.SUPERLIKE_MONTHLY_REDISTRIBUTION.name())
        );
    }
}
