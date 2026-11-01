package fr.uge.project.ruge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "WALLET")
public class WalletEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "MONEY", nullable = false, precision = 19, scale = 2)
    private BigDecimal money;

    @Size(max = 50)
    @NotNull
    @Column(name = "TYPE", nullable = false, length = 50)
    private String type;
}
