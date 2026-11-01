package fr.uge.project.ruge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "FOLLOW", schema = "PUBLIC")
public class FollowEntity {
    @EmbeddedId
    private FollowIdEntity id;

    @MapsId("followerId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "FOLLOWER_ID", nullable = false)
    private UserEntity follower;

    @MapsId("followedId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "FOLLOWED_ID", nullable = false)
    private UserEntity followed;

    @NotNull
    @Column(name = "DATE_FOLLOW", nullable = false)
    private LocalDateTime dateFollow;

    @Size(max = 255)
    @Column(name = "FOLLOW_KEY")
    private String followKey;


}
