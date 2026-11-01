package fr.uge.project.ruge.dto;

public record ProfilePhotoDto(
    Long id,
    String code,
    String label,
    String imageUrl
) {}
