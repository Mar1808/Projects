package fr.uge.project.ruge.services;

import fr.uge.project.ruge.dto.UserLikeReactionDto;
import fr.uge.project.ruge.entity.LikerEntity;
import fr.uge.project.ruge.entity.LikerIdEntity;
import fr.uge.project.ruge.exceptions.RecipeNotFoundException;
import fr.uge.project.ruge.exceptions.UserNotFoundException;
import fr.uge.project.ruge.repositories.LikerRepository;
import fr.uge.project.ruge.repositories.RecipeRepository;
import fr.uge.project.ruge.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Service
public class LikerService {
    private final LikerRepository likerRepository;
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;

    public LikerService(LikerRepository likerRepository,
                        UserRepository userRepository,
                        RecipeRepository recipeRepository) {
        this.likerRepository = Objects.requireNonNull(likerRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.recipeRepository = Objects.requireNonNull(recipeRepository);
    }

    @Transactional(readOnly = true)
    public boolean hasLiked(Long userId, Long recipeId) {
        if (userId == null || recipeId == null) {
            return false;
        }
        return likerRepository.existsBySender_IdAndRecipe_Id(userId, recipeId);
    }

    @Transactional(readOnly = true)
    public long countLikes(Long recipeId) {
        if (recipeId == null) {
            return 0L;
        }
        return likerRepository.countByRecipe_Id(recipeId);
    }

    @Transactional(readOnly = true)
    public List<UserLikeReactionDto> getLikesBySenderId(Long senderId) {
        Objects.requireNonNull(senderId);
        return likerRepository.findBySender_IdOrderByDateDesc(senderId).stream()
                .map(liker -> new UserLikeReactionDto(
                        liker.getRecipe().getId(),
                        liker.getRecipe().getName(),
                        liker.getDate()
                ))
                .toList();
    }

    @Transactional
    public void like(Long senderId, Long recipeId) {
        if (senderId == null || recipeId == null) {
            throw new IllegalArgumentException("Invalid user or recipe.");
        }
        if (likerRepository.existsBySender_IdAndRecipe_Id(senderId, recipeId)) {
            return;
        }

        var sender = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + senderId));
        var recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException("Recipe not found: " + recipeId));

        if (recipe.getCreator() == null) {
            throw new IllegalArgumentException("Cannot like a recipe without an author.");
        }
        if (Objects.equals(recipe.getCreator().getId(), senderId)) {
            throw new IllegalArgumentException("You cannot like your own recipe.");
        }

        var liker = new LikerEntity();
        var id = new LikerIdEntity();
        id.setSenderId(senderId);
        id.setReceiverId(recipe.getCreator().getId());
        id.setRecipeId(recipeId);

        liker.setId(id);
        liker.setSender(sender);
        liker.setReceiver(recipe.getCreator());
        liker.setRecipe(recipe);
        liker.setDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));

        likerRepository.save(liker);
    }

    @Transactional
    public void unlike(Long senderId, Long recipeId) {
        if (senderId == null || recipeId == null) {
            return;
        }
        likerRepository.deleteBySender_IdAndRecipe_Id(senderId, recipeId);
    }
}

