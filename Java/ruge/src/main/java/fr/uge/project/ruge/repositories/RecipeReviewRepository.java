package fr.uge.project.ruge.repositories;

import fr.uge.project.ruge.entity.RecipeReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeReviewRepository extends JpaRepository<RecipeReviewEntity, Long> {

    List<RecipeReviewEntity> findByRecipe_IdOrderByDateReviewDescIdDesc(Long recipeId);

    List<RecipeReviewEntity> findByAuthor_IdOrderByDateReviewDescIdDesc(Long authorId);
}
