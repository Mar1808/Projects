package fr.uge.project.ruge.repositories;

import fr.uge.project.ruge.entity.RecipeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence access for recipes.
 */
@Repository
public interface RecipeRepository extends JpaRepository<RecipeEntity, Long> {
    long countByActiveTrue();

    long countByActiveFalse();

    @Query("SELECT r FROM RecipeEntity r " +
        "LEFT JOIN FETCH r.creator " +
        "LEFT JOIN FETCH r.steps " +
        "WHERE r.id = :id")
    Optional<RecipeEntity> findByIdWithSteps(@Param("id") Long id);

    @Query("SELECT r FROM RecipeEntity r " +
        "LEFT JOIN FETCH r.ingredients " +
        "WHERE r.id = :id")
    Optional<RecipeEntity> findByIdWithIngredients(@Param("id") Long id);

    /**
     * Returns newest active recipes with pagination.
     */
    Page<RecipeEntity> findByActiveTrueOrderByIdDesc(Pageable pageable);

    /**
     * Returns active recipes by creator, newest first.
     */
    List<RecipeEntity> findByCreator_IdAndActiveTrueOrderByIdDesc(Long creatorId);

    /**
     * Returns active recipes by creator, newest first, with pagination.
     */
    Page<RecipeEntity> findByCreator_IdAndActiveTrueOrderByIdDesc(Long creatorId, Pageable pageable);

    /**
     * Searches recipes by recipe name or ingredient name (case-insensitive).
     * Uses joins to avoid N+1 issues.
     */
    @Query("SELECT DISTINCT r FROM RecipeEntity r " +
        "LEFT JOIN FETCH r.creator " +
        "LEFT JOIN FETCH r.steps " +
        "LEFT JOIN r.ingredients i " +
        "WHERE r.active = true " +
        "AND (LOWER(r.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
        "OR LOWER(i.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<RecipeEntity> findByRecipeOrIngredientName(String searchTerm, Pageable pageable);

    @Modifying
    @Query("UPDATE RecipeEntity r SET r.active = false WHERE r.id = :id")
    void deactivateRecipe(Long id);

    @Query("SELECT DISTINCT r FROM RecipeEntity r LEFT JOIN FETCH r.ingredients LEFT JOIN FETCH r.steps")
    List<RecipeEntity> findAllWithIngredients();

    /**
     * List version of recipe search by recipe name or ingredient name (case-insensitive).
     */
    @Query("SELECT DISTINCT r FROM RecipeEntity r " +
        "LEFT JOIN FETCH r.creator " +
        "LEFT JOIN FETCH r.steps " +
        "LEFT JOIN r.ingredients i " +
        "WHERE r.active = true " +
        "AND (LOWER(r.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
        "OR LOWER(i.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<RecipeEntity> findByRecipeOrIngredientNameListed(String searchTerm);

    @Query("SELECT DISTINCT r FROM RecipeEntity r LEFT JOIN FETCH r.steps WHERE r.id IN :ids")
    List<RecipeEntity> findAllByIdsWithSteps(@Param("ids") List<Long> ids);
}
