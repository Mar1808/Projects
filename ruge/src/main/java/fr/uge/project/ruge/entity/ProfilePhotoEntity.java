package fr.uge.project.ruge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "PROFILE_PHOTO")
public class ProfilePhotoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "CODE", nullable = false, unique = true, length = 100)
    private String code;

    @NotNull
    @Column(name = "LABEL", nullable = false, length = 100)
    private String label;

    @NotNull
    @Column(name = "IMAGE_URL", nullable = false, length = 255)
    private String imageUrl;

    @NotNull
    @Column(name = "ENABLED", nullable = false)
    private Boolean enabled = true;

    @NotNull
    @Column(name = "DISPLAY_ORDER", nullable = false)
    private Integer displayOrder = 0;
}
