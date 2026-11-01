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
@Table(name = "LIKER", schema = "PUBLIC")
public class LikerEntity {
    @EmbeddedId
    private LikerIdEntity id;

    @MapsId("senderId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "SENDER_ID", nullable = false)
    private UserEntity sender;

    @MapsId("receiverId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "RECEIVER_ID", nullable = false)
    private UserEntity receiver;

    @MapsId("recipeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "RECIPE_ID", nullable = false)
    private RecipeEntity recipe;

    @NotNull
    @Column(name = "DATE", nullable = false)
    private LocalDateTime date;


}
