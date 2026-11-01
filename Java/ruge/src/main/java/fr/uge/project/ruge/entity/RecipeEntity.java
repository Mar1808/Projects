package fr.uge.project.ruge.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

/**
 * Entity representing a recipe and its related components.
 */
@Getter
@Setter
@Entity
@Table(name = "RECIPE")
public class RecipeEntity {

  private static final String DELETED_PREFIXE = "__DELETED__" ;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;
  private String description;
  private boolean active;

  @Column(name = "image_url")
  private String imageUrl;

  @ManyToOne
  @JoinColumn(name = "CREATOR_USERID", foreignKey = @ForeignKey(name = "FK_CREATOR_ID"))
  private UserEntity creator;

  // English alias for existing French field name, kept for backward compatibility.
  @ManyToOne
  @JoinColumn(name = "variant_id")
  private RecipeEntity variant;

  @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<InstructionEntity> steps;

  @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<IngredientEntity> ingredients;

  public RecipeEntity() {
  }

  public int getTotalDuration() {
    return steps.stream().filter(s -> !s.getStep().startsWith(DELETED_PREFIXE)).map(InstructionEntity::getDuration).reduce(0, Integer::sum);
  }

  @Deprecated(forRemoval = false)
  public void setVariante(RecipeEntity variante) {
    this.variant = variante;
  }
}
