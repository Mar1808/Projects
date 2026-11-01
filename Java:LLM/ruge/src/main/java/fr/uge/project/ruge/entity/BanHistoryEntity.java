package fr.uge.project.ruge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "BAN_HISTORY", schema = "PUBLIC")
public class BanHistoryEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Column(name = "BANNED_USER_ID", nullable = false)
  private Long bannedUserId;

  @NotNull
  @Column(name = "BANNED_BY_ID", nullable = false)
  private Long bannedById;

  @NotNull
  @Column(name = "BAN_DATE", nullable = false)
  private LocalDateTime banDate;

  @NotNull
  @Column(name = "REASON", nullable = false)
  private String reason;

  // Ban duration in minutes (-1 means permanent ban).
  private int duration;

  public BanHistoryEntity() {
  }

  public boolean isActive() {
    if (duration == -1) {
      return true; // Permanent ban.
    }
    var expirationDate = banDate.plusMinutes(duration);
    return LocalDateTime.now().isBefore(expirationDate);
  }
}
