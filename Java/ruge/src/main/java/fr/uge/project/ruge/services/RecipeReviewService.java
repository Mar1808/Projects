package fr.uge.project.ruge.services;

import fr.uge.project.ruge.dto.RecipeReviewDto;
import fr.uge.project.ruge.dto.UserReviewReactionDto;
import fr.uge.project.ruge.entity.RecipeReviewEntity;
import fr.uge.project.ruge.exceptions.RecipeNotFoundException;
import fr.uge.project.ruge.exceptions.UserNotFoundException;
import fr.uge.project.ruge.repositories.RecipeRepository;
import fr.uge.project.ruge.repositories.RecipeReviewRepository;
import fr.uge.project.ruge.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Service
public class RecipeReviewService {

    private final RecipeReviewRepository recipeReviewRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    public RecipeReviewService(RecipeReviewRepository recipeReviewRepository,
                               RecipeRepository recipeRepository,
                               UserRepository userRepository) {
        this.recipeReviewRepository = Objects.requireNonNull(recipeReviewRepository);
        this.recipeRepository = Objects.requireNonNull(recipeRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    @Transactional(readOnly = true)
    public List<RecipeReviewDto> getReviewsByRecipeId(Long recipeId) {
        return recipeReviewRepository.findByRecipe_IdOrderByDateReviewDescIdDesc(recipeId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserReviewReactionDto> getReviewsByAuthorId(Long authorId) {
        Objects.requireNonNull(authorId);
        return recipeReviewRepository.findByAuthor_IdOrderByDateReviewDescIdDesc(authorId).stream()
                .map(this::toUserReactionDto)
                .toList();
    }

    @Transactional
    public void saveReviewComment(Long userId, Long recipeId, String reviewComment) {
        if (userId == null || recipeId == null) {
            throw new IllegalArgumentException("Invalid user or recipe.");
        }
        String normalizedComment = reviewComment == null ? null : reviewComment.trim();
        if (normalizedComment == null || normalizedComment.isBlank()) {
            throw new IllegalArgumentException("Review cannot be empty.");
        }
        if (normalizedComment.length() > 1000) {
            throw new IllegalArgumentException("Review cannot exceed 1000 characters.");
        }

        var author = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        var recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException("Recipe not found: " + recipeId));

        RecipeReviewEntity review = new RecipeReviewEntity();
        review.setAuthor(author);
        review.setRecipe(recipe);
        review.setComment(normalizedComment);
        review.setDateReview(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));

        recipeReviewRepository.save(review);
    }

    private RecipeReviewDto toDto(RecipeReviewEntity review) {
        return RecipeReviewDto.builder()
                .id(review.getId())
                .authorId(review.getAuthor().getId())
                .authorName(review.getAuthor().getName())
                .comment(review.getComment())
                .dateReview(review.getDateReview())
                .build();
    }

    private UserReviewReactionDto toUserReactionDto(RecipeReviewEntity review) {
        return new UserReviewReactionDto(
                review.getRecipe().getId(),
                review.getRecipe().getName(),
                review.getComment(),
                review.getDateReview()
        );
    }
}
