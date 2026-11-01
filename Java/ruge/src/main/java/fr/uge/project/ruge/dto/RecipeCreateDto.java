package fr.uge.project.ruge.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO used to create a recipe.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeCreateDto {

    @NotBlank(message = "Recipe name is required")
    @Size(min = 1, max = 100, message = "Name must contain between 1 and 100 characters")
    private String name;

    @NotBlank(message = "Description is required")
    @Size(min = 1, max = 2000, message = "Description must contain between 1 and 2000 characters")
    private String description;

    private String imageUrl;

    @Valid
    @NotNull(message = "Steps are required")
    @Size(min = 1, message = "At least one step is required")
    private List<InstructionDto> steps;

    @Valid
    @NotNull(message = "Ingredients are required")
    @Size(min = 1, message = "At least one ingredient is required")
    private List<IngredientDto> ingredients;
}
