package fr.uge.project.ruge.dto;

import java.util.List;

public record UserHistoryResponseDto(
    UserDisplayDto user,
    List<BanHistoryDto> history
) {}