package fr.uge.project.ruge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientDto {

  private Long id;

  @NotBlank(message = "Ingredient name is required")
  private String name;

  @NotBlank(message = "Quantity is required")
  private String quantity;
}
