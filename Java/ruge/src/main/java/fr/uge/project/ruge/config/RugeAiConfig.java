package fr.uge.project.ruge.config;

import fr.uge.project.ruge.entity.IngredientEntity;
import fr.uge.project.ruge.entity.RecipeEntity;
import fr.uge.project.ruge.repositories.RecipeRepository;
import fr.uge.project.ruge.services.RecipeAiService;
import org.springframework.ai.document.Document;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Configuration
public class RugeAiConfig {

    /*
     * CommandLineRunner used to index existing recipes in the VectorStore at startup.
     */
    @Bean
    @Transactional
    public CommandLineRunner indexOldRecipes(RecipeRepository recipeRepository, RecipeAiService recipeAiService) {
        return args -> {
            System.out.println("[AI INDEXING] Starting synchronization...");

            List<RecipeEntity> allRecipes = recipeRepository.findAllWithIngredients();
            if (allRecipes.isEmpty()) {
                System.out.println("No recipe found in database, indexing canceled.");
                return;
            }

            System.out.println("INDEXING INFO: " + allRecipes.size() + " recipes to process.");
            List<Document> documentsToIndex = new ArrayList<>();

            for (RecipeEntity recipe : allRecipes) {
                try {
                    String ingredients = recipe.getIngredients().stream()
                        .map(IngredientEntity::getName)
                        .collect(Collectors.joining(", "));

                    String validUuid = UUID.nameUUIDFromBytes(String.valueOf(recipe.getId()).getBytes()).toString();

                    Document doc = recipeAiService.generateRecipeDocument(
                        validUuid,
                        recipe.getId(),
                        recipe.getName(),
                        recipe.getDescription(),
                        ingredients,
                            recipe.getTotalDuration()
                    );

                    documentsToIndex.add(doc);
                    System.out.println("Document prepared: " + recipe.getName());
                } catch (Exception e) {
                    System.err.println("Error preparing recipe " + recipe.getId() + ": " + e.getMessage());
                }
            }

            if (!documentsToIndex.isEmpty()) {
                System.out.println("[AI INDEXING] Computing vectors and saving...");
                recipeAiService.saveDocuments(documentsToIndex);
            }

            System.out.println("[AI INDEXING] Synchronization completed successfully.");
        };
    }
}
