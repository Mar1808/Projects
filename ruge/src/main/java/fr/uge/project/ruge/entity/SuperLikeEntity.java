package fr.uge.project.ruge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "SUPER_LIKE", schema = "PUBLIC", indexes = {
        @Index(name = "IDX_SUPERLIKE_SENDER",
                columnList = "SENDER_ID"),
        @Index(name = "IDX_SUPERLIKE_RECEIVER",
                columnList = "RECEIVER_ID"),
        @Index(name = "IDX_SUPERLIKE_RECIPE",
                columnList = "RECIPE_ID")})
public class SuperLikeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "SENDER_ID", nullable = false)
    private UserEntity sender;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "RECEIVER_ID", nullable = false)
    private UserEntity receiver;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "RECIPE_ID", nullable = false)
    private RecipeEntity recipe;

    @NotNull
    @Column(name = "AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @NotNull
    @ColumnDefault("FALSE")
    @Column(name = "IS_REDISTRIBUTED", nullable = false)
    private Boolean isRedistributed;

    @NotNull
    @Column(name = "DATE", nullable = false)
    private LocalDateTime date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROMOTION_ID")
    private PromotionEntity promotion;


}
