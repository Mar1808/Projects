package fr.uge.project.ruge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeReviewDto {
    private Long id;
    private Long authorId;
    private String authorName;
    private String comment;
    private LocalDateTime dateReview;
}
