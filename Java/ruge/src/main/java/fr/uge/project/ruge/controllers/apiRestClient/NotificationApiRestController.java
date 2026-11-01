package fr.uge.project.ruge.controllers.apiRestClient;

import fr.uge.project.ruge.entity.NotificationEntity;
import fr.uge.project.ruge.repositories.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications") // Correspond à l'interface Retrofit
public class NotificationApiRestController {

    private final NotificationRepository notificationRepository;

    public NotificationApiRestController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/latest")
    public ResponseEntity<List<NotificationEntity>> getLatest() {
        // On renvoie les 5 dernières notifications enregistrées par les Observers
        return ResponseEntity.ok(notificationRepository.findTop1ByOrderByCreatedAtDesc());
    }
}