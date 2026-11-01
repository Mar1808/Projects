package fr.uge.project.ruge.services;

import fr.uge.project.ruge.entity.BanHistoryEntity;
import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.enums.Status;
import fr.uge.project.ruge.repositories.BanHistoryRepository;
import fr.uge.project.ruge.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountLockServiceTest {
  @Mock
  private UserRepository userRepository;
  @Mock
  private BanHistoryRepository banHistoryRepository;

  @Test
  void refreshAndIsLockedReturnsFalseForActiveUser() {
    var service = new AccountLockService(userRepository, banHistoryRepository);
    var user = user(1L, Status.ACTIVE);

    var locked = service.refreshAndIsLocked(user);

    assertThat(locked).isFalse();
    verifyNoInteractions(banHistoryRepository);
    verify(userRepository, never()).save(user);
  }

  @Test
  void refreshAndIsLockedReturnsTrueWhenBanIsActive() {
    var service = new AccountLockService(userRepository, banHistoryRepository);
    var user = user(2L, Status.BANNED);
    var activeBan = banHistory(user.getId(), LocalDateTime.now().minusMinutes(5), 120);
    when(banHistoryRepository.findTopByBannedUserIdOrderByBanDateDesc(user.getId())).thenReturn(Optional.of(activeBan));

    var locked = service.refreshAndIsLocked(user);

    assertThat(locked).isTrue();
    verify(userRepository, never()).save(user);
  }

  @Test
  void refreshAndIsLockedUnbansWhenLatestBanExpired() {
    var service = new AccountLockService(userRepository, banHistoryRepository);
    var user = user(3L, Status.BANNED);
    var expiredBan = banHistory(user.getId(), LocalDateTime.now().minusHours(3), 30);
    when(banHistoryRepository.findTopByBannedUserIdOrderByBanDateDesc(user.getId())).thenReturn(Optional.of(expiredBan));

    var locked = service.refreshAndIsLocked(user);

    assertThat(locked).isFalse();
    assertThat(user.getStatus()).isEqualTo(Status.ACTIVE);
    verify(userRepository).save(user);
  }

  @Test
  void refreshAndIsLockedUnbansWhenHistoryIsMissing() {
    var service = new AccountLockService(userRepository, banHistoryRepository);
    var user = user(4L, Status.DELETED);
    when(banHistoryRepository.findTopByBannedUserIdOrderByBanDateDesc(user.getId())).thenReturn(Optional.empty());

    var locked = service.refreshAndIsLocked(user);

    assertThat(locked).isFalse();
    assertThat(user.getStatus()).isEqualTo(Status.ACTIVE);
    verify(userRepository).save(user);
  }

  private static UserEntity user(Long id, Status status) {
    var user = new UserEntity();
    user.setId(id);
    user.setStatus(status);
    return user;
  }

  private static BanHistoryEntity banHistory(Long userId, LocalDateTime banDate, int durationMinutes) {
    var banHistory = new BanHistoryEntity();
    banHistory.setBannedUserId(userId);
    banHistory.setBanDate(banDate);
    banHistory.setDuration(durationMinutes);
    banHistory.setBannedById(99L);
    banHistory.setReason("Violation");
    return banHistory;
  }
}
