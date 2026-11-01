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
import lombok.ToString;

/**
 * Entity representing one specific recipe step.
 */
@Getter
@Setter
@Entity
@ToString
@Table(name = "INSTRUCTION")
public class InstructionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "recipe_id")
  private RecipeEntity recipe;

  private String step;
  private int duration;

  public InstructionEntity() {
  }
}
