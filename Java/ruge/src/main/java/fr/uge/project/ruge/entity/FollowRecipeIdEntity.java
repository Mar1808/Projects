package fr.uge.project.ruge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class FollowRecipeIdEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = -7251294049413369095L;
    @NotNull
    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @NotNull
    @Column(name = "RECIPE_ID", nullable = false)
    private Long recipeId;


}