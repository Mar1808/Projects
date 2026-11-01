package fr.uge.project.ruge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for compact recipe display in lists.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleRecipeDto {
    private Long id;
    private String name;
    private String imageUrl;
    private Integer totalDuration;
    private String creatorName;
}
