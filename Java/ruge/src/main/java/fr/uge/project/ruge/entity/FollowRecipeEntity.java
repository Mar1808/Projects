package fr.uge.project.ruge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "FOLLOW_RECIPE", schema = "PUBLIC")
public class FollowRecipeEntity {
    @EmbeddedId
    private FollowRecipeIdEntity id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity user;

    @MapsId("recipeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "RECIPE_ID", nullable = false)
    private RecipeEntity recipe;

    @NotNull
    @Column(name = "DATE", nullable = false)
    private LocalDateTime date;


}
