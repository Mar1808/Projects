package fr.uge.xplain.controllers;

import com.github.tjake.jlama.model.AbstractModel;
import com.github.tjake.jlama.model.ModelSupport;
import com.github.tjake.jlama.safetensors.DType;
import com.github.tjake.jlama.safetensors.prompt.PromptContext;
import com.github.tjake.jlama.util.Downloader;
import fr.uge.xplain.compilation.Compiler;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/llm")
public final class LLM {
    private AbstractModel abstractModel;
    private final String[] tabModels = {
            "tjake/Llama-3.2-1B-Instruct-JQ4",
            "tjake/Llama-3.2-3B-Instruct-JQ4",
            "tjake/Llama-3.1-8B-Instruct-JQ4"
    };
    private final Map<String, Integer> models = new HashMap<>(Map.of(
            "tjake/Llama-3.2-1B-Instruct-JQ4", 0,
            "tjake/Llama-3.2-3B-Instruct-JQ4", 0,
            "tjake/Llama-3.1-8B-Instruct-JQ4", 0
    ));
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final Object lock = new Object();

    public AbstractModel abstractModel() {
        synchronized (lock) {
            return abstractModel;
        }
    }

    public Map<String, Integer> models() {
        synchronized (lock) {
            return models;
        }
    }

    public void loadModel(int modelIndex) throws IOException {
        synchronized (lock) {
            String workingDirectory = "./models";
            File modelFile = new Downloader(workingDirectory, tabModels[modelIndex]).huggingFaceModel();
            models.put(tabModels[modelIndex], 1);
            if (!modelFile.exists()) {
                throw new IllegalStateException("Model file does not exist.");
            }
            abstractModel = ModelSupport.loadModel(modelFile, DType.F32, DType.I8);
        }

    }

    @GetMapping("/llm-correction")
    public SseEmitter correctionWithSSE(@RequestParam String contentClass, @RequestParam String contentErrorMessage, @RequestParam String llmSelected) throws IOException {
        SseEmitter emitter = new SseEmitter(300_000L);
        int modelIndex = switchHelper(llmSelected);
        ensureModelLoaded(modelIndex);
        String promptContext = "Here is the class " + contentClass + ". When compiling this class, the following error occurs: "
                + contentErrorMessage + ". Correct the class to resolve the error. Provide only the corrected version of the class.";
        handleSseEmitter(emitter, promptContext, true);
        return emitter;
    }

    private int switchHelper(String llmSelected) {
        synchronized (lock) {
            return switch (llmSelected) {
                case "rapide" -> 0;
                case "moyen" -> 1;
                case "lent" -> 2;
                default -> throw new IllegalStateException("Unexpected value: " + llmSelected);
            };
        }
    }

    private void ensureModelLoaded(int modelIndex) throws IOException {
        synchronized (lock) {
            if(modelIndex > 2 || modelIndex < 0){
                throw new IllegalStateException("error modelIndex");
            }
            if (models.get(tabModels[modelIndex]) == 0) {
                loadModel(modelIndex);
            }
            if (abstractModel == null) {
                throw new IllegalStateException("Model not loaded. Call loadModel() first.");
            }
        }

    }

    private String buildPromptContext(String contentClass, Compiler compiler) {
        synchronized (lock) {
            return "Here is the class " + contentClass + ", when compiling this class, the following error occurs: "
                    + compiler.getErrorMessage() + ". Without providing a fix, explain the cause of this error and why it occurs.";
        }
    }

    private void handleSseEmitter(SseEmitter emitter, String promptContext, boolean isCorrection) {
        executorService.execute(() -> {
            try {
                generateTokens(emitter, promptContext, isCorrection);
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
    }

    @GetMapping("/llm-response")
    public SseEmitter promptResponseWithSSE(@RequestParam String llmSelected, @RequestParam String contentClass) throws IOException {
        synchronized (lock) {
            int modelIndex = switchHelper(llmSelected);
            Compiler compiler = new Compiler(contentClass);
            boolean compilationSuccessful = compiler.compile();
            if (!compilationSuccessful) {
                ensureModelLoaded(modelIndex);
                SseEmitter emitter = new SseEmitter(300_000L);
                String promptContext = buildPromptContext(contentClass, compiler);
                handleSseEmitter(emitter, promptContext, false);
                return emitter;
            }
            SseEmitter emitter = new SseEmitter();
            emitter.complete();
            return emitter;
        }
    }

    private void generateTokens(SseEmitter emitter, String promptContext, boolean isCorrection) throws IOException {
        PromptContext context = createPromptContext(promptContext, isCorrection);
        abstractModel.generate(UUID.randomUUID(), context, 0.3f, 512, (token, _) -> {
            try {
                emitter.send(new String(token.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private PromptContext createPromptContext(String promptContext, boolean isCorrection) {
        Objects.requireNonNull(promptContext, "Prompt context cannot be null");

        if (abstractModel.promptSupport().isPresent()) {
            String systemMessage = isCorrection
                    ? "You are an advanced Java assistant. Provide only the corrected version of the code without any explanations or additional tokens."
                    : "You are a professional in Java. Analyze the errors and explain their causes. Short answer only, and no code.";
            return abstractModel.promptSupport()
                    .get()
                    .builder()
                    .addSystemMessage(systemMessage)
                    .addUserMessage(promptContext)
                    .build();
        } else {
            return PromptContext.of(promptContext);
        }
    }

    @GetMapping("/compile-error")
    public String getCompilationError(@RequestParam String contentClass) {
        synchronized (lock) {
            Compiler compiler = new Compiler(contentClass);
            if (!compiler.compile()) {
                return compiler.getErrorMessage();
            }
            return "SUCCESS";
        }
    }
}