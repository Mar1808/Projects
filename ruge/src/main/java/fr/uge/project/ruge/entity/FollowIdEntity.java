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
public class FollowIdEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 3655103388238261795L;
    @NotNull
    @Column(name = "FOLLOWER_ID", nullable = false)
    private Long followerId;

    @NotNull
    @Column(name = "FOLLOWED_ID", nullable = false)
    private Long followedId;


}