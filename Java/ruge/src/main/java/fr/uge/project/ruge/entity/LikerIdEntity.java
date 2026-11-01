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
public class LikerIdEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = -1365062032964180172L;
    @NotNull
    @Column(name = "SENDER_ID", nullable = false)
    private Long senderId;

    @NotNull
    @Column(name = "RECEIVER_ID", nullable = false)
    private Long receiverId;

    @NotNull
    @Column(name = "RECIPE_ID", nullable = false)
    private Long recipeId;


}