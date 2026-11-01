package fr.uge.xplain.compilation;

import javax.tools.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.*;

public final class Compiler {
    private final String contentClass;
    private final String[] options = {"-d", "out"};
    private String errorMessage = "";

    public Compiler(String contentClass) {
        this.contentClass = Objects.requireNonNull(contentClass, "Le code source ne peut pas être nul");
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Extrait le nom de la classe principale du code source.
     *
     * @param contentClass Le code source sous forme de chaîne.
     * @return Le nom de la classe principale.
     * @throws IllegalArgumentException si le nom de la classe ne peut pas être trouvé.
     */
    public String getNameClass(String contentClass) {
        Objects.requireNonNull(contentClass);
        Pattern pattern = Pattern.compile("\\b(class|record|enum|interface|@interface)\\s+([a-zA-Z0-9_]+)");
        Matcher matcher = pattern.matcher(contentClass);
        if (matcher.find()) {
            return matcher.group().split(" ")[1];
        }
        return "Erreur de syntaxe, veuillez revoir le nom et la déclaration de la classe";
    }

    /**
     * Compile le code Java passé en paramètre.
     *
     * @return true si la compilation réussit, false sinon.
     */
    public boolean compile() {
        String className = getNameClass(contentClass); // Le nom de la classe
        if (className.equals("Erreur de syntaxe, veuillez revoir le nom et la déclaration de la classe")) {
            errorMessage = "Erreur de syntaxe, veuillez revoir le nom et la déclaration de la classe";
            return false;
        }

        File tempFile = null;
        try {
            // Création du fichier temporaire
            tempFile = new File(className + ".java");

            // Écriture du contenu dans le fichier
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile.toPath())) {
                writer.write(contentClass);
            }

            // Obtention du compilateur Java
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new IllegalStateException("Le compilateur Java n'est pas disponible. Utilisez un JDK, pas un JRE.");
            }

            // Configuration des diagnostics pour collecter les erreurs
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
                Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(tempFile);
                JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, Arrays.asList(options), null, compilationUnits);

                // Exécution de la compilation
                boolean success = task.call();
                if (!success) {
                    StringBuilder errors = new StringBuilder("Erreurs de compilation détectées :\n");
                    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                        errors.append("Erreur à la ligne ")
                                .append(diagnostic.getLineNumber())
                                .append(": ")
                                .append(diagnostic.getMessage(null))
                                .append("\n");
                    }
                    errorMessage = errors.toString();
                }
                return success;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Erreur lors de la gestion des fichiers", e);
        } finally {
            // Suppression sécurisée du fichier temporaire
            if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
                System.err.println("Impossible de supprimer le fichier temporaire : " + tempFile.getAbsolutePath());
            }
        }
    }
}