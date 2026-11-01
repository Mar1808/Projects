package fr.uge.project.ruge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "app_configuration")
public class AppConfigurationEntity {

    @Id
    private Integer id = 1;

    @NotNull
    @Column(name = "ai_search_price", nullable = false)
    private BigDecimal aiSearchPrice;

    @NotNull
    @Column(name = "super_like_price", nullable = false)
    private BigDecimal superLikePrice;

    @NotNull
    @Column(name = "stripe_min_amount", nullable = false)
    private BigDecimal stripeMinAmount;

    @NotNull
    @Column(name = "stripe_max_amount", nullable = false)
    private BigDecimal stripeMaxAmount;

    @Column(name = "terms_content", columnDefinition = "TEXT")
    private String termsContent;
}
