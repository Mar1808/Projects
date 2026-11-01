package fr.uge.project.ruge.repositories;

import fr.uge.project.ruge.entity.LikerEntity;
import fr.uge.project.ruge.entity.LikerIdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LikerRepository extends JpaRepository<LikerEntity, LikerIdEntity> {
  boolean existsBySender_IdAndRecipe_Id(Long senderId, Long recipeId);

  long countByRecipe_Id(Long recipeId);

  void deleteBySender_IdAndRecipe_Id(Long senderId, Long recipeId);

  List<LikerEntity> findBySender_IdOrderByDateDesc(Long senderId);
}

