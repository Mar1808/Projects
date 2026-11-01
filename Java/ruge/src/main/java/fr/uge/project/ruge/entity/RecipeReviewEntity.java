package fr.uge.project.ruge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "RECIPE_REVIEW")
public class RecipeReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "RECIPE_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_RECIPE_REVIEW_RECIPE"))
    private RecipeEntity recipe;

    @ManyToOne
    @JoinColumn(name = "USER_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_RECIPE_REVIEW_USER"))
    private UserEntity author;

    @Column(name = "COMMENT", nullable = false, length = 1000)
    private String comment;

    @Column(name = "DATE_REVIEW", nullable = false)
    private LocalDateTime dateReview;
}
