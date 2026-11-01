package fr.uge.project.ruge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "SEARCH_LOG", schema = "PUBLIC", indexes = {
        @Index(name = "IDX_SEARCHLOG_USER",
                columnList = "USER_ID"),
        @Index(name = "IDX_SEARCHLOG_DATE",
                columnList = "DATE")})
public class SearchLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity user;

    @Column(name = "FOLLOWED_ID")
    private Long followedId;

    @NotNull
    @Column(name = "DATE", nullable = false)
    private LocalDateTime date;

    @Column(name = "AMOUNT", precision = 19, scale = 2)
    private BigDecimal amount;


}
