package fr.uge.project.ruge.repositories;

import fr.uge.project.ruge.entity.FollowRecipeEntity;
import fr.uge.project.ruge.entity.FollowRecipeIdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowRecipeRepository extends JpaRepository<FollowRecipeEntity, FollowRecipeIdEntity> {
  boolean existsByUserIdAndRecipeId(Long userId, Long recipeId);

  void deleteByUserIdAndRecipeId(Long userId, Long recipeId);
}

