package fr.uge.project.ruge.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for full recipe details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeDetailDto {
    private Long id;
    private String name;
    private String description;
    @Size(max = 255, message = "Image URL must be less than 255 characters")
    private String imageUrl;
    private String creatorName;
    private Long creatorId;
    private List<InstructionDto> steps;
    private List<IngredientDto> ingredients;
    private int totalDuration;
    private Long recipeSourceId;
    private boolean active;
}
