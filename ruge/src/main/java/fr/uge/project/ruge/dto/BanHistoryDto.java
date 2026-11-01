package fr.uge.project.ruge.dto;

import java.time.LocalDateTime;

public record BanHistoryDto(
    Long id,
    Long bannedUserId,
    Long bannedById,
    LocalDateTime banDate,
    String reason,
    int duration
) {
}
