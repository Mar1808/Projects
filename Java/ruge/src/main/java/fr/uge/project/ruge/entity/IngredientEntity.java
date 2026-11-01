package fr.uge.project.ruge.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing an ingredient and its quantity for a recipe.
 */
@Getter
@Setter
@Entity
@Table(name = "INGREDIENT")
public class IngredientEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "recipe_id")
  private RecipeEntity recipe;

  private String name;
  private String quantity;

  public IngredientEntity() {
  }
}
