package fr.uge.project.ruge.services;

import fr.uge.project.ruge.entity.FollowRecipeEntity;
import fr.uge.project.ruge.entity.FollowRecipeIdEntity;
import fr.uge.project.ruge.exceptions.RecipeNotFoundException;
import fr.uge.project.ruge.exceptions.UserNotFoundException;
import fr.uge.project.ruge.repositories.FollowRecipeRepository;
import fr.uge.project.ruge.repositories.RecipeRepository;
import fr.uge.project.ruge.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Service
public class FollowRecipeService {
    private final FollowRecipeRepository followRecipeRepository;
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;

    public FollowRecipeService(FollowRecipeRepository followRecipeRepository,
                               UserRepository userRepository,
                               RecipeRepository recipeRepository) {
        this.followRecipeRepository = Objects.requireNonNull(followRecipeRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.recipeRepository = Objects.requireNonNull(recipeRepository);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(Long userId, Long recipeId) {
        if (userId == null || recipeId == null) {
            return false;
        }
        return followRecipeRepository.existsByUserIdAndRecipeId(userId, recipeId);
    }

    @Transactional
    public void follow(Long userId, Long recipeId) {
        if (userId == null || recipeId == null) {
            throw new IllegalArgumentException("Invalid user or recipe.");
        }
        if (followRecipeRepository.existsByUserIdAndRecipeId(userId, recipeId)) {
            return;
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        var recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException("Recipe not found: " + recipeId));
        if (recipe.getCreator() != null && Objects.equals(recipe.getCreator().getId(), userId)) {
            throw new IllegalArgumentException("You cannot follow your own recipe.");
        }

        var follow = new FollowRecipeEntity();
        var id = new FollowRecipeIdEntity();
        id.setUserId(userId);
        id.setRecipeId(recipeId);
        follow.setId(id);
        follow.setUser(user);
        follow.setRecipe(recipe);
        follow.setDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        followRecipeRepository.save(follow);
    }

    @Transactional
    public void unfollow(Long userId, Long recipeId) {
        if (userId == null || recipeId == null) {
            return;
        }
        followRecipeRepository.deleteByUserIdAndRecipeId(userId, recipeId);
    }
}

