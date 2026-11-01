package fr.uge.project.ruge.services;

import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.enums.Status;
import fr.uge.project.ruge.repositories.BanHistoryRepository;
import fr.uge.project.ruge.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class AccountLockService {
    private final UserRepository userRepository;
    private final BanHistoryRepository banHistoryRepository;

    public AccountLockService(UserRepository userRepository, BanHistoryRepository banHistoryRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.banHistoryRepository = Objects.requireNonNull(banHistoryRepository);
    }

    @Transactional
    public boolean refreshAndIsLocked(UserEntity user) {
        Objects.requireNonNull(user);
        var isNotActive = user.getStatus() != null && user.getStatus() != Status.ACTIVE;
        if (!isNotActive) {
            return false;
        }

        var latestBanOpt = banHistoryRepository.findTopByBannedUserIdOrderByBanDateDesc(user.getId());
        if (latestBanOpt.isPresent()) {
            if (!latestBanOpt.get().isActive()) {
                setActive(user);
                return false;
            }
            return true;
        }

        setActive(user);
        return false;
    }

    private void setActive(UserEntity user) {
        user.setStatus(Status.ACTIVE);
        userRepository.save(user);
    }
}
