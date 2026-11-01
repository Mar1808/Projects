package fr.uge.project.ruge.repositories;

import fr.uge.project.ruge.entity.SuperLikeEntity;
import fr.uge.project.ruge.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SuperLikeRepository extends JpaRepository<SuperLikeEntity, Long> {
  long countByRecipe_Id(Long recipeId);

  boolean existsBySender_IdAndRecipe_Id(Long id, Long id1);

  @Query("""
      SELECT sl
      FROM SuperLikeEntity sl
      JOIN FETCH sl.recipe r
      LEFT JOIN FETCH r.creator
      WHERE sl.isRedistributed = false
      AND sl.date >= :periodStart
      AND sl.date < :periodEnd
      """)
  List<SuperLikeEntity> findAllPendingForPeriod(@Param("periodStart") LocalDateTime periodStart,
                                                @Param("periodEnd") LocalDateTime periodEnd);

  @Query("""
      SELECT sl
      FROM SuperLikeEntity sl
      JOIN FETCH sl.recipe r
      LEFT JOIN FETCH r.creator
      WHERE sl.isRedistributed = false
      """)
  List<SuperLikeEntity> findAllPending();

  long countBySenderAndDateAfter(UserEntity sender, LocalDateTime date);

  @Query("SELECT s FROM SuperLikeEntity s " +
      "JOIN FETCH s.recipe " +
      "JOIN FETCH s.receiver " +
      "WHERE s.sender.id = :userId")
  List<SuperLikeEntity> findBySender_Id(Long userId);
}
