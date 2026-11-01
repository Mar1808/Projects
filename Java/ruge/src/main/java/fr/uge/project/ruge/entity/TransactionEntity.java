package fr.uge.project.ruge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
@Entity
@Table(name = "TRANSACTION", schema = "PUBLIC", indexes = {
        @Index(name = "IDX_TX_SOURCE_WALLET",
                columnList = "SOURCE_WALLET_ID"),
        @Index(name = "IDX_TX_DEST_WALLET",
                columnList = "DESTINATION_WALLET_ID"),
        @Index(name = "IDX_TX_DATE",
                columnList = "DATE"),
        @Index(name = "IDX_TX_TYPE",
                columnList = "TRANSACTION_TYPE"),
        @Index(name = "IDX_TX_SOURCE_RECIPE",
                columnList = "SOURCE_RECIPE_ID"),
        @Index(name = "IDX_TX_TARGET_RECIPE",
                columnList = "TARGET_RECIPE_ID")},
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_TX_PROVIDER_PAYMENT",
                        columnNames = {"PROVIDER", "PROVIDER_PAYMENT_ID"})})
public class TransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "SOURCE_WALLET_ID", nullable = false)
    private WalletEntity sourceWallet;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "DESTINATION_WALLET_ID", nullable = false)
    private WalletEntity destinationWallet;

    @NotNull
    @Column(name = "AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Size(max = 1000)
    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    @Column(name = "REFERENCE_ID")
    private Long referenceId;

    @Size(max = 50)
    @Column(name = "TRANSACTION_TYPE", length = 50)
    private String transactionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "SOURCE_RECIPE_ID")
    private RecipeEntity sourceRecipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "TARGET_RECIPE_ID")
    private RecipeEntity targetRecipe;

    @Size(max = 50)
    @Column(name = "PROVIDER", length = 50)
    private String provider;

    @Size(max = 255)
    @Column(name = "PROVIDER_PAYMENT_ID", length = 255)
    private String providerPaymentId;

    @NotNull
    @Column(name = "DATE", nullable = false)
    private LocalDateTime date;

    @PrePersist
    void setDefaultDateIfMissing() {
        if (date == null) {
            date = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        }
    }

}
