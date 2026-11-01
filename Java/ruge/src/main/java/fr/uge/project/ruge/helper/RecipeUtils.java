package fr.uge.project.ruge.helper;

import fr.uge.project.ruge.dto.IngredientDto;
import fr.uge.project.ruge.dto.InstructionDto;
import fr.uge.project.ruge.dto.RecipeDetailDto;

import java.util.List;

public final class RecipeUtils {

    private static final String KEEPED_PREFIX = "__KEEPED__";
    private static final String DELETED_PREFIX = "__DELETED__";

    private RecipeUtils() {}

    public static List<IngredientDto> cleanVariantIngredients(List<IngredientDto> ingredients) {
        if (ingredients == null) return List.of();

        return ingredients.stream()
            .filter(i -> !i.getName().startsWith(DELETED_PREFIX))
            .map(i -> {
                if (i.getName().startsWith(KEEPED_PREFIX)) {
                    return IngredientDto.builder()
                        .id(i.getId())
                        .name(i.getName().substring(KEEPED_PREFIX.length()))
                        .quantity(i.getQuantity())
                        .build();
                }
                return i;
            })
            .toList();
    }

  public static List<InstructionDto> cleanVariantInstructions(List<InstructionDto> instructions) {
    if (instructions == null) return List.of();

    return instructions.stream()
            .filter(i -> !i.getStep().startsWith(DELETED_PREFIX))
            .map(i -> {
              if (i.getStep().startsWith(KEEPED_PREFIX)) {
                return InstructionDto.builder()
                        .id(i.getId())
                        .step(i.getStep().substring(KEEPED_PREFIX.length()))
                        .duration(i.getDuration())
                        .build();
              }
              return i;
            })
            .toList();
  }

  public static RecipeDetailDto clean(RecipeDetailDto recipe) {
    if (recipe == null) return null;

    recipe.setIngredients(cleanVariantIngredients(recipe.getIngredients()));
    recipe.setSteps(cleanVariantInstructions(recipe.getSteps()));

    return recipe;
  }
}
