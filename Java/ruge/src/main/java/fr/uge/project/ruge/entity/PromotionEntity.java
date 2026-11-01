package fr.uge.project.ruge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "PROMOTION", schema = "PUBLIC")
public class PromotionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "DATE_START", nullable = false)
    private LocalDateTime dateStart;

    @NotNull
    @Column(name = "DATE_END", nullable = false)
    private LocalDateTime dateEnd;

    @Size(max = 2000)
    @Column(name = "DESCRIPTION", length = 2000)
    private String description;

    @Column(name = "TYPE", nullable = false)
    private String type;

    @Column(name = "PROMO_PRICE", precision = 19, scale = 2)
    private BigDecimal promoPrice;

    public boolean isActive() {
        LocalDateTime today = LocalDateTime.now();
        return this.dateStart != null && this.dateEnd != null
                && !today.isBefore(this.dateStart)
                && !today.isAfter(this.dateEnd);
    }
}