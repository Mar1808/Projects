package fr.uge.xplain.controllers;

import fr.uge.xplain.bd.Explanation;
import fr.uge.xplain.bd.ExplanationRepository;
import fr.uge.xplain.compilation.Compiler;
import fr.uge.xplain.dto.JavaClassRequest;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("")
public class XplainController {
    private final ExplanationRepository explanationRepository;

    public XplainController(ExplanationRepository explanationRepository) {
        this.explanationRepository = explanationRepository;
    }

    @GetMapping("/history")
    public List<Explanation> getAllData() {
        return explanationRepository.findByOrderByIdDesc(); // Renvoie directement les objets Explanation
    }

    @PostMapping("/data")
    public void Save(@RequestBody JavaClassRequest request) throws IOException {
        System.out.println("Requête reçue : " + request);
        String javaClass = request.getClassJava();
        String errors = request.getErrors();
        String correction = request.getCorrection();
        String xplanation = request.getXplanation();
        var entity = new Explanation(javaClass, errors, xplanation, correction);
        explanationRepository.save(entity);
    }
}
