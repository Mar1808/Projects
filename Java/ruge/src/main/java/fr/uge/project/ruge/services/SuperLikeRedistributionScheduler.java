package fr.uge.project.ruge.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class SuperLikeRedistributionScheduler {
    private final SuperLikeService superLikeService;

    public SuperLikeRedistributionScheduler(SuperLikeService superLikeService) {
        this.superLikeService = Objects.requireNonNull(superLikeService);
    }

    @Scheduled(cron = "${app.superlike.redistribution.cron:0 5 0 1 * *}", zone = "${app.superlike.redistribution.zone:Europe/Paris}")
    public void redistributePreviousMonth() {
        superLikeService.redistributePreviousMonth();
    }
}
