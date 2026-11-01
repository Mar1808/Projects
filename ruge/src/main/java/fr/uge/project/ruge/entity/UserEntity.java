package fr.uge.project.ruge.entity;

import fr.uge.project.ruge.enums.Role;
import fr.uge.project.ruge.enums.SsoProvider;
import fr.uge.project.ruge.enums.Status;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * JPA entity representing an application user (APP_USER table).
 */
@Getter
@Setter
@Entity
@Table(name = "APP_USER")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "EMAIL", nullable = false, length = 255)
    private String email;

    @NotNull
    @Column(name = "NAME", nullable = false, length = 255)
    private String name;

    @NotNull
    @Column(name = "PASSWORD", nullable = false, length = 255)
    private String password;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false, length = 50)
    private Role role;

    @Column(name = "IMAGE_URL", length = 255)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROFILE_PHOTO_ID")
    private ProfilePhotoEntity profilePhoto;

    @Enumerated(EnumType.STRING)
    @Column(name = "SSO_PROVIDER", length = 50)
    private SsoProvider ssoProvider;

    @Column(name = "SSO_ID", length = 255)
    private String ssoId;

    @Column(name = "STRIPE_CONNECT_ACCOUNT_ID", length = 255)
    private String stripeConnectAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @OneToMany(mappedBy = "bannedUserId")
    private List<BanHistoryEntity> banHistory;

    public UserEntity() {
    }

    @OneToOne(
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @JoinColumn(
        name = "WALLET_ID",
        unique = true
    )
    private WalletEntity wallet;
}
