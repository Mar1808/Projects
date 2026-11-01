package fr.uge.project.ruge.services;

import fr.uge.project.ruge.dto.SimpleRecipeDto;
import fr.uge.project.ruge.entity.RecipeEntity;
import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.mapper.RecipeMapper;
import fr.uge.project.ruge.repositories.RecipeRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;



@Service
public class RecipeAiService {

    private final VectorStore vectorStore;
    private final RecipeRepository recipeRepository;
    private final RecipeMapper recipeMapper = new RecipeMapper();

    private final ChatClient chatClient;
    private final WalletService walletService;

    private static final int RELEVANT_RECIPES = 12;
    private static final float SIMILARITY_THRESHOLD = 0.3f;

    public RecipeAiService(VectorStore vectorStore, RecipeRepository recipeRepository, ChatClient.Builder chatClientBuilder, WalletService walletService) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = Objects.requireNonNull(vectorStore);
        this.recipeRepository = Objects.requireNonNull(recipeRepository);
        this.walletService = Objects.requireNonNull(walletService);
    }

    public Document generateRecipeDocument(String uuid, Long originalId, String name, String description, String ingredients, int duree) {

        String categorisationPrompt = """
        Tu es LIMITÉ à 30 mots, ne fais pas des phrases, fais UNIQUEMENT et strictement des mots clés. NE DÉPASSE PAS 50 MOTS.
        Analyse cette recette : "%s".
        Donne moi uniquement une liste de 6 mots-clés séparés par des virgules qui définissent :
        son origine, son type (sucré/salé), sa catégorie (pâtisserie, plat, entrée), son style et un groupe de mot clés contenant les intolérences alimentaires.
        Il existe aussi un paramètre de durée, que la recette devra respecter approximativement, et qui doit être pris en compte dans les mots-clés.
        """.formatted(name + " " + description);

        String tags = chatClient.prompt().user(categorisationPrompt).call().content();

        String formattedText = String.format(
                "NOM : %s. \nINGRÉDIENTS : %s .\nTAGS : %s.\nDUREE : %s",
                name, ingredients, tags, duree
        );

        return Document.builder()
                .id(uuid)
                .text(formattedText)
                .metadata("recipe_id", originalId)
                .build();
    }

    public void saveDocuments(List<Document> documents) {
        if (documents != null && !documents.isEmpty()) {
            vectorStore.add(documents);
        }
    }

    public void indexRecipe(String uuid, Long originalId, String name, String description, String ingredients,  int duree) {
        Document doc = generateRecipeDocument(uuid, originalId, name, description, ingredients, duree);

        // On découpe le document en blocs de 400 tokens max (avec un chevauchement de 50)
        // pour rester largement sous la limite des 512 tokens de mxbai-embed-large
        TokenTextSplitter splitter = new TokenTextSplitter(400, 50, 5, 10000, true);
        List<Document> chunks = splitter.apply(List.of(doc));

        saveDocuments(chunks);
        System.out.println("Indexé : " + name + " (découpé en " + chunks.size() + " morceaux)");
    }

    @Transactional
    public List<SimpleRecipeDto> askRecipe(UserEntity user, String research) {
        if(!walletService.payForSearchAi(user)) {
            throw new RuntimeException("Solde insuffisant pour effectuer cette recherche IA.");
        }

        SearchRequest request = SearchRequest.builder()
                .query(research)
                .topK(RELEVANT_RECIPES)
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .build();

        List<Document> documents = vectorStore.similaritySearch(request);

        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        // On récupère la liste des IDs DANS LE BON ORDRE (Ordre de pertinence IA)
        List<Long> recipeIds = documents.stream()
                .map(doc -> doc.getMetadata().get("recipe_id"))
                .filter(Objects::nonNull)
                .map(id -> Long.valueOf(id.toString()))
                .distinct()
                .toList();

        // On récupère les recettes en base
        var recipesFromDb = recipeRepository.findAllByIdsWithSteps(recipeIds);

        // On mappe pour un accès rapide
        Map<Long, RecipeEntity> recipeMap = recipesFromDb.stream()
                .collect(Collectors.toMap(recipe -> {
                    try {
                        return (Long) recipe.getClass().getMethod("getId").invoke(recipe);
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                }, r -> r));

        // On re-construit la liste de retour en respectant strictement l'ordre du VectorStore
        return recipeIds.stream()
                .map(recipeMap::get)
                .filter(Objects::nonNull)
                .map(recipe -> recipeMapper.toSimpleRecipeDto((RecipeEntity) recipe)) // Adapte le cast selon ton entité
                .toList();
    }
}