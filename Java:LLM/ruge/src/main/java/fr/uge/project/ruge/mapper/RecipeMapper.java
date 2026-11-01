package fr.uge.project.ruge.mapper;

import fr.uge.project.ruge.dto.IngredientDto;
import fr.uge.project.ruge.dto.InstructionDto;
import fr.uge.project.ruge.dto.RecipeCreateDto;
import fr.uge.project.ruge.dto.RecipeDetailDto;
import fr.uge.project.ruge.dto.SimpleRecipeDto;
import fr.uge.project.ruge.entity.IngredientEntity;
import fr.uge.project.ruge.entity.InstructionEntity;
import fr.uge.project.ruge.entity.RecipeEntity;
import fr.uge.project.ruge.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper converting Recipe entities to DTOs and vice versa.
 */
@Component
public class RecipeMapper {

    /**
     * Converts RecipeEntity to SimpleRecipeDto.
     */
    public SimpleRecipeDto toSimpleRecipeDto(RecipeEntity entity) {
        if (entity == null) {
            return null;
        }

        return SimpleRecipeDto.builder()
            .id(entity.getId())
            .name(entity.getName())
            .imageUrl(entity.getImageUrl())
            .totalDuration(entity.getTotalDuration())
            .creatorName(entity.getCreator() != null ? entity.getCreator().getName() : null)
            .build();
    }

    /**
     * Converts a RecipeEntity list to SimpleRecipeDto list.
     */
    public List<SimpleRecipeDto> toSimpleRecipeDto(List<RecipeEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }

        return entities.stream()
            .map(this::toSimpleRecipeDto)
            .collect(Collectors.toList());
    }

    /**
     * Converts RecipeEntity to RecipeDetailDto.
     */
    public RecipeDetailDto toDetailDto(RecipeEntity entity) {
        if (entity == null) {
            return null;
        }

        List<InstructionDto> steps = toInstructionDtoList(entity.getSteps());
        int totalDuration = steps.stream()
            .mapToInt(InstructionDto::getDuration)
            .sum();

        return RecipeDetailDto.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .imageUrl(entity.getImageUrl())
            .creatorName(entity.getCreator() != null ? entity.getCreator().getName() : null)
            .creatorId(entity.getCreator() != null ? entity.getCreator().getId() : null)
            .steps(steps)
            .ingredients(toIngredientDtoList(entity.getIngredients()))
            .totalDuration(totalDuration)
            .recipeSourceId(entity.getVariant() != null ? entity.getVariant().getId() : null)
            .active(entity.isActive())
            .build();
    }

    /**
     * Converts RecipeCreateDto to RecipeEntity.
     */
    public RecipeEntity toEntity(RecipeCreateDto dto, UserEntity creator) {
        if (dto == null) {
            return null;
        }

        RecipeEntity entity = new RecipeEntity();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setImageUrl(dto.getImageUrl());
        entity.setCreator(creator);
        return entity;
    }

    /**
     * Converts InstructionEntity list to InstructionDto list (ordered by id).
     */
    private List<InstructionDto> toInstructionDtoList(List<InstructionEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }

        return entities.stream()
            .sorted(Comparator.comparing(InstructionEntity::getId))
            .map(this::toInstructionDto)
            .collect(Collectors.toList());
    }

    /**
     * Converts InstructionEntity to InstructionDto.
     */
    private InstructionDto toInstructionDto(InstructionEntity entity) {
        if (entity == null) {
            return null;
        }

        return InstructionDto.builder()
            .id(entity.getId())
            .step(entity.getStep())
            .duration(entity.getDuration())
            .build();
    }

    /**
     * Converts IngredientEntity set to IngredientDto list.
     */
    private List<IngredientDto> toIngredientDtoList(Set<IngredientEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }

        return entities.stream()
            .sorted(Comparator.comparing(IngredientEntity::getId))
            .map(this::toIngredientDto)
            .collect(Collectors.toList());
    }

    /**
     * Converts IngredientEntity to IngredientDto.
     */
    private IngredientDto toIngredientDto(IngredientEntity entity) {
        if (entity == null) {
            return null;
        }

        return IngredientDto.builder()
            .id(entity.getId())
            .name(entity.getName())
            .quantity(entity.getQuantity())
            .build();
    }

    /**
     * Creates InstructionEntity from InstructionDto.
     */
    public InstructionEntity toInstructionEntity(InstructionDto dto, RecipeEntity recipe) {
        if (dto == null) {
            return null;
        }

        InstructionEntity entity = new InstructionEntity();
        entity.setStep(dto.getStep());
        entity.setDuration(dto.getDuration());
        entity.setRecipe(recipe);
        return entity;
    }

    /**
     * Creates IngredientEntity from IngredientDto.
     */
    public IngredientEntity toIngredientEntity(IngredientDto dto, RecipeEntity recipe) {
        if (dto == null) {
            return null;
        }

        IngredientEntity entity = new IngredientEntity();
        entity.setName(dto.getName());
        entity.setQuantity(dto.getQuantity());
        entity.setRecipe(recipe);
        return entity;
    }
}
