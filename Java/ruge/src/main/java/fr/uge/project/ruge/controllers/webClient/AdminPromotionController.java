package fr.uge.project.ruge.controllers.webClient;

import fr.uge.project.ruge.services.PromotionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Controller
@RequestMapping("/admin")
public class AdminPromotionController {

    private final PromotionService promotionService;

    public AdminPromotionController(PromotionService promotionService) {
        this.promotionService = Objects.requireNonNull(promotionService);
    }

    @GetMapping("/promotions")
    public String showPromotions(Model model) {
        model.addAttribute("promotions", promotionService.getAllPromotions());
        return "admin/promotion";
    }

    @PostMapping("/promotions")
    public String createPromotion(
            @RequestParam String type,
            @RequestParam BigDecimal promoPrice,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            RedirectAttributes redirectAttributes) {

        try {
            promotionService.createPromotion(type, promoPrice, startDate, endDate);
            redirectAttributes.addFlashAttribute("successMessage", "Promotion créée avec succès !");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/promotions";
    }

    @PostMapping("/promotions/{id}/delete")
    public String deletePromotion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            promotionService.deletePromotion(id);
            redirectAttributes.addFlashAttribute("successMessage", "Promotion supprimée.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/promotions";
    }

    @GetMapping("/promotion/{id}/modify")
    public String modifyPromotion(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("promotion", promotionService.getPromotionById(id));
            return "admin/modifyPromo";

        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/promotions";
        }
    }

    @PostMapping("/promotion/{id}/modify")
    public String updatePromotion(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal promoPrice,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            RedirectAttributes redirectAttributes) {

        try {
            promotionService.updatePromotion(id, promoPrice, startDate, endDate);
            redirectAttributes.addFlashAttribute("successMessage", "Promotion modifiée avec succès !");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/promotions";
    }
}