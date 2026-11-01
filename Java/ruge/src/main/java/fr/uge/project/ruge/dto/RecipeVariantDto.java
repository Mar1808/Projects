package fr.uge.project.ruge.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@Getter
@ToString
public class RecipeVariantDto {

    private Long originalRecipeId;

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @Valid
    @NotNull
    private List<VariantStepDto> steps;

    @Valid
    @NotNull
    private List<VariantIngredientDto> ingredients;

    @Setter
    @Getter
    @ToString
    public static class VariantStepDto {

        // Aligned with InstructionDto: field name is "step".
        @NotBlank
        private String step;

        private int duration;
        private boolean deleted;
        private boolean original;
    }

    @Setter
    @Getter
    @ToString
    public static class VariantIngredientDto {

        @NotBlank
        private String name;

        private String quantity;
        private boolean deleted;
        private boolean original;
    }
}
