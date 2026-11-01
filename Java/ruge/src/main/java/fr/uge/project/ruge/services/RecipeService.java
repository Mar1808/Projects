package fr.uge.project.ruge.services;

import fr.uge.project.ruge.dto.*;
import fr.uge.project.ruge.entity.IngredientEntity;
import fr.uge.project.ruge.entity.InstructionEntity;
import fr.uge.project.ruge.entity.RecipeEntity;
import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.exceptions.RecipeNotFoundException;
import fr.uge.project.ruge.mapper.RecipeMapper;
import fr.uge.project.ruge.repositories.RecipeRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

import static fr.uge.project.ruge.helper.RecipeUtils.clean;

/**
 * Service handling recipe business logic.
 */
@Slf4j
@Service
public class RecipeService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final RecipeRepository recipeRepository;
    private final RecipeMapper recipeMapper;
    private final RecipeAiService recipeAiService;

    public RecipeService(RecipeRepository recipeRepository,
                         RecipeMapper recipeMapper,
                         RecipeAiService recipeAiService) {
        this.recipeRepository = Objects.requireNonNull(recipeRepository);
        this.recipeMapper = Objects.requireNonNull(recipeMapper);
        this.recipeAiService = Objects.requireNonNull(recipeAiService);
    }

    /**
     * Returns one page of recipes (20 per page, newest first).
     *
     * @param page page index, starting from 0
     * @return paged simple recipes
     */
    @Transactional(readOnly = true)
    public Page<SimpleRecipeDto> getRecipes(int page) {
        log.info("Fetching recipe page {} (size: {})", page, DEFAULT_PAGE_SIZE);
        if (page < 0) {
            log.warn("Invalid page index: {}. Page index must be non-negative.", page);
            page = 0;
        }
        Pageable pageable = PageRequest.of(page, DEFAULT_PAGE_SIZE);
        Page<RecipeEntity> recipePage = recipeRepository.findByActiveTrueOrderByIdDesc(pageable);

        log.info("{} recipe(s) found on page {}/{}",
                recipePage.getNumberOfElements(),
                page + 1,
                recipePage.getTotalPages());

        return recipePage.map(recipeMapper::toSimpleRecipeDto);
    }

    /**
     * Returns the first 20 recipes (page 0).
     */
    @Transactional(readOnly = true)
    public List<SimpleRecipeDto> getAllRecipes(int page) {
        return getRecipes(page).getContent();
    }

    @Transactional(readOnly = true)
    public List<RecipeDetailDto> getAllDetailedRecipes() {
        return getRecipes(0).getContent().stream()
                .map(SimpleRecipeDto::getId)
                .map(this::getRecipeById)
                .collect(Collectors.toList());
    }

    /**
     * Returns full details of one recipe.
     *
     * @param id recipe id
     * @return full recipe details
     */
    @Transactional(readOnly = true)
    public RecipeDetailDto getRecipeById(Long id) {
        log.info("Fetching recipe details for id {}", id);

        RecipeEntity recipe = recipeRepository.findByIdWithSteps(id)
                .orElseThrow(() -> {
                    log.error("Recipe not found for id {}", id);
                    return new RecipeNotFoundException("Recipe not found for id: " + id);
                });
        recipeRepository.findByIdWithIngredients(id).orElseThrow(() -> {
            log.error("Ingredients not found for recipe id {}", id);
            return new RecipeNotFoundException("Ingredients not found for recipe id: " + id);
        });

        log.info("Recipe found: {} ({} ingredients, {} steps)",
                recipe.getName(),
                recipe.getIngredients().size(),
                recipe.getSteps().size());

        return recipeMapper.toDetailDto(recipe);
    }

    @Transactional(readOnly = true)
    public RecipeDetailDto getRecipeByIdForVariant(Long id) {
        return clean(getRecipeById(id));
    }

    @Deprecated()
    @Transactional(readOnly = true)
    public RecipeDetailDto getRecipeByIdForVariante(Long id) {
        return getRecipeByIdForVariant(id);
    }

    /**
     * Creates a recipe with ingredients and steps, then indexes it in the vector store.
     */
    @Transactional
    public RecipeDetailDto createRecipe(RecipeCreateDto createDto, UserEntity currentUser) {
        if (currentUser == null) {
            return null;
        }
        log.info("Creating a new recipe: {} by user {}", createDto.getName(), currentUser.getId());

        if (createDto.getIngredients() == null || createDto.getIngredients().isEmpty()) {
            throw new IllegalArgumentException("At least one ingredient is required.");
        }
        if (createDto.getSteps() == null || createDto.getSteps().isEmpty()) {
            throw new IllegalArgumentException("At least one step is required.");
        }

        var recipe = recipeMapper.toEntity(createDto, currentUser);
        recipe = recipeRepository.save(recipe);
        log.info("Recipe created with id {}", recipe.getId());

        final var instructions = new ArrayList<InstructionEntity>();
        for (InstructionDto dto : createDto.getSteps()) {
            instructions.add(recipeMapper.toInstructionEntity(dto, recipe));
        }
        recipe.setSteps(instructions);

        final var ingredients = new HashSet<IngredientEntity>();
        for (IngredientDto dto : createDto.getIngredients()) {
            ingredients.add(recipeMapper.toIngredientEntity(dto, recipe));
        }
        recipe.setIngredients(ingredients);
        recipe.setActive(true);

        recipe = recipeRepository.save(recipe);

        try {
            String ingredientsForAi = ingredients.stream()
                    .map(IngredientEntity::getName)
                    .collect(Collectors.joining(", "));

            String vectorId = UUID.nameUUIDFromBytes(String.valueOf(recipe.getId()).getBytes()).toString();
            recipeAiService.indexRecipe(
                    vectorId,
                    recipe.getId(),
                    recipe.getName(),
                    recipe.getDescription(),
                    ingredientsForAi,
                    recipe.getTotalDuration()
            );
            log.info("Recipe indexed successfully in the vector database.");
        } catch (Exception e) {
            log.warn("AI indexing failed for recipe id {}. Recipe still saved. Error: {}",
                    recipe.getId(), e.getMessage());
        }

        log.info("Recipe created successfully: {} ({} ingredients, {} steps)",
                recipe.getName(),
                ingredients.size(),
                instructions.size());

        return recipeMapper.toDetailDto(recipe);
    }

    public RecipeDetailDto createRecipe(RecipeCreateDto createDto, UserEntity user, MultipartFile imageFile) {

        // Si une image a bien été envoyée depuis le téléphone
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                Path uploadPath = Paths.get("uploads/");
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                String fileName = UUID.randomUUID().toString() + "_" + imageFile.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);

                Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                String fileUrl = "http://10.0.2.2:8080/uploads/" + fileName;
                createDto.setImageUrl(fileUrl);

            } catch (IOException e) {
                throw new RuntimeException("Erreur lors de la sauvegarde de l'image : " + e.getMessage(), e);
            }
        }
        return createRecipe(createDto, user);
    }

    @Transactional(readOnly = true)
    public List<SimpleRecipeDto> getRecipesByCreatorId(Long creatorId) {
        Objects.requireNonNull(creatorId);
        return recipeMapper.toSimpleRecipeDto(
                recipeRepository.findByCreator_IdAndActiveTrueOrderByIdDesc(creatorId)
        );
    }

    @Transactional(readOnly = true)
    public Page<SimpleRecipeDto> getRecipesByCreatorIdPageable(Long creatorId, int page) {
        Objects.requireNonNull(creatorId);
        Pageable pageable = PageRequest.of(page, DEFAULT_PAGE_SIZE);
        var recipePage = recipeRepository.findByCreator_IdAndActiveTrueOrderByIdDesc(creatorId, pageable);
        return recipePage.map(recipeMapper::toSimpleRecipeDto);
    }

    @Transactional(readOnly = true)
    @NotBlank
    public Page<SimpleRecipeDto> searchRecipe(String query, int page) {
        Objects.requireNonNull(query);
        if (page < 0) {
            log.warn("Invalid page index: {}. Page index must be non-negative.", page);
            page = 0;
        }
        Pageable pageable = PageRequest.of(page, DEFAULT_PAGE_SIZE); // Page Reques
        return recipeRepository.findByRecipeOrIngredientName(query, pageable)
                .map(recipeMapper::toSimpleRecipeDto);
    }

    @Deprecated()
    @Transactional(readOnly = true)
    @NotBlank
    public Page<SimpleRecipeDto> researchRecipe(String research) {
        return searchRecipe(research, 0);
    }

    @Transactional
    public void deleteRecipe(Long id) {
        if (!recipeRepository.existsById(id)) {
            log.warn("Recipe not found for delete, id {}", id);
            throw new RecipeNotFoundException("Recipe not found for id: " + id);
        }
        recipeRepository.deactivateRecipe(id);
    }

    @Transactional
    public RecipeDetailDto createVariant(RecipeVariantDto variantDto, UserEntity creator) {
        Objects.requireNonNull(variantDto);
        Objects.requireNonNull(creator);

        RecipeEntity originalRecipe = recipeRepository.findById(variantDto.getOriginalRecipeId())
                .orElseThrow(() -> new RecipeNotFoundException(
                        "Original recipe not found for id: " + variantDto.getOriginalRecipeId()));

        RecipeEntity variant = new RecipeEntity();
        variant.setName(variantDto.getName());
        variant.setCreator(creator);
        variant.setVariant(originalRecipe);
        variant.setActive(true);
        variant.setDescription(variantDto.getDescription());

        variant = recipeRepository.save(variant);

        final var steps = new ArrayList<InstructionEntity>();
        for (RecipeVariantDto.VariantStepDto stepDto : variantDto.getSteps()) {
            steps.add(getInstructionEntity(stepDto, variant));
        }
        variant.setSteps(new ArrayList<>(steps));

        final var ingredients = new HashSet<IngredientEntity>();
        for (RecipeVariantDto.VariantIngredientDto ingDto : variantDto.getIngredients()) {
            IngredientEntity ingredient = new IngredientEntity();
            String name = ingDto.getName();

            if (ingDto.isDeleted()) {
                ingredient.setName("__DELETED__" + name);
            } else if (ingDto.isOriginal()) {
                ingredient.setName("__KEEPED__" + name);
            } else {
                ingredient.setName(name);
            }

            ingredient.setQuantity(ingDto.getQuantity());
            ingredient.setRecipe(variant);
            ingredients.add(ingredient);
        }
        variant.setIngredients(ingredients);

        variant = recipeRepository.save(variant);
        log.info("Variant created successfully: {} (original: {})", variant.getName(), originalRecipe.getId());
        return recipeMapper.toDetailDto(variant);
    }

    private static @NonNull InstructionEntity getInstructionEntity(RecipeVariantDto.VariantStepDto stepDto, RecipeEntity variant) {
        InstructionEntity step = new InstructionEntity();
        String description = stepDto.getStep();

        if (stepDto.isDeleted()) {
            step.setStep("__DELETED__" + description);
        } else if (stepDto.isOriginal()) {
            step.setStep("__KEEPED__" + description);
        } else {
            step.setStep(description);
        }

        step.setRecipe(variant);
        step.setDuration(stepDto.getDuration());
        return step;
    }

    @Transactional(readOnly = true)
    @NotBlank
    public List<SimpleRecipeDto> searchRecipeListed(String query) {
        Objects.requireNonNull(query);
        return recipeRepository.findByRecipeOrIngredientNameListed(query).stream()
                .map(recipeMapper::toSimpleRecipeDto)
                .collect(Collectors.toList());
    }

    @Deprecated()
    @Transactional(readOnly = true)
    @NotBlank
    public List<SimpleRecipeDto> researchRecipeListed(String research) {
        return searchRecipeListed(research);
    }

    /**
     * Returns full details of one recipe.
     *
     * @param id recipe id
     * @return full recipe details
     */
    @Transactional(readOnly = true)
    public RecipeDetailDto getRecipeByIdModifyRight(Long id, UserEntity currentUser) {
        log.info("Fetching recipe details for id {}", id);

        RecipeEntity recipe = recipeRepository.findByIdWithSteps(id)
                .orElseThrow(() -> {
                    log.error("Recipe not found for id {}", id);
                    return new RecipeNotFoundException("Recipe not found for id: " + id);
                });
        recipeRepository.findByIdWithIngredients(id).orElseThrow(() -> {
            log.error("Ingredients not found for recipe id {}", id);
            return new RecipeNotFoundException("Ingredients not found for recipe id: " + id);
        });

        log.info("Recipe found: {} ({} ingredients, {} steps)",
                recipe.getName(),
                recipe.getIngredients().size(),
                recipe.getSteps().size());

        if (!recipe.getCreator().getId().equals(currentUser.getId())) {
            log.warn("User {} does not have modify rights for recipe {}", currentUser.getId(), id);
            throw new SecurityException("You do not have permission to modify this recipe.");
        }
        return recipeMapper.toDetailDto(recipe);
    }

    @Transactional
    public RecipeDetailDto updateRecipe(Long id, RecipeDetailDto recipeDto, UserEntity currentUser) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(recipeDto);
        Objects.requireNonNull(currentUser);

        log.info("Updating recipe: {} by user {}", recipeDto.getName(), currentUser.getId());

        RecipeEntity recipe = recipeRepository.findByIdWithSteps(id)
                .orElseThrow(() -> {
                    log.error("Recipe not found for id {}", id);
                    return new RecipeNotFoundException("Recipe not found for id: " + id);
                });

        if (!recipe.getCreator().getId().equals(currentUser.getId())) {
            log.warn("User {} does not have modify rights for recipe {}", currentUser.getId(), id);
            throw new SecurityException("You do not have permission to modify this recipe.");
        }

        recipe.setName(recipeDto.getName());
        recipe.setDescription(recipeDto.getDescription());
        recipe.setImageUrl(recipeDto.getImageUrl());

        if (recipeDto.getIngredients() != null && !recipeDto.getIngredients().isEmpty()) {
            recipe.getIngredients().clear();
            for (IngredientDto ingredientDto : recipeDto.getIngredients()) {
                IngredientEntity ingredient = new IngredientEntity();
                ingredient.setName(ingredientDto.getName());
                ingredient.setQuantity(ingredientDto.getQuantity());
                ingredient.setRecipe(recipe);
                recipe.getIngredients().add(ingredient);
            }
        } else {
            throw new IllegalArgumentException("At least one ingredient is required.");
        }

        if (recipeDto.getSteps() != null && !recipeDto.getSteps().isEmpty()) {
            recipe.getSteps().clear();
            for (InstructionDto stepDto : recipeDto.getSteps()) {
                InstructionEntity instruction = new InstructionEntity();
                instruction.setStep(stepDto.getStep());
                instruction.setDuration(stepDto.getDuration());
                instruction.setRecipe(recipe);
                recipe.getSteps().add(instruction);
            }
        } else {
            throw new IllegalArgumentException("At least one step is required.");
        }

        recipe = recipeRepository.save(recipe);
        log.info("Recipe updated successfully: {} ({} ingredients, {} steps)",
                recipe.getName(),
                recipe.getIngredients().size(),
                recipe.getSteps().size());

        return recipeMapper.toDetailDto(recipe);
    }


}
