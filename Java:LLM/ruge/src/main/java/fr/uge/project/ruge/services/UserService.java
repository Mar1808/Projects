package fr.uge.project.ruge.services;

import fr.uge.project.ruge.dto.BanHistoryDto;
import fr.uge.project.ruge.dto.ProfilePhotoDto;
import fr.uge.project.ruge.dto.UserDisplayDto;
import fr.uge.project.ruge.dto.UserDto;
import fr.uge.project.ruge.entity.BanHistoryEntity;
import fr.uge.project.ruge.entity.ProfilePhotoEntity;
import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.entity.WalletEntity;
import fr.uge.project.ruge.enums.Role;
import fr.uge.project.ruge.enums.WalletType;
import fr.uge.project.ruge.exceptions.UserAlreadyExistsException;
import fr.uge.project.ruge.exceptions.UserNotFoundException;
import fr.uge.project.ruge.repositories.BanHistoryRepository;
import fr.uge.project.ruge.repositories.ProfilePhotoRepository;
import fr.uge.project.ruge.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static fr.uge.project.ruge.enums.Status.*;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BanHistoryRepository banHistoryRepository;
    private final ProfilePhotoRepository profilePhotoRepository;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       BanHistoryRepository banHistoryRepository,
                       ProfilePhotoRepository profilePhotoRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
        this.banHistoryRepository = Objects.requireNonNull(banHistoryRepository);
        this.profilePhotoRepository = Objects.requireNonNull(profilePhotoRepository);
    }

    @Transactional
    public void registerUser(UserDto user) {
        Objects.requireNonNull(user);

        if (userRepository.findByEmail(user.email()).isPresent()) {
            throw new UserAlreadyExistsException(user.email() + " already exists");
        }

        var wallet = new WalletEntity();
        wallet.setMoney(BigDecimal.ZERO);
        wallet.setType(WalletType.USER.name());

        var userToAdd = new UserEntity();
        userToAdd.setEmail(user.email());
        userToAdd.setName(user.name());
        userToAdd.setRole(Role.MEMBER);
        userToAdd.setPassword(passwordEncoder.encode(user.password()));
        var profilePhoto = resolveProfilePhotoForRegistration(user.profilePhotoId());
        userToAdd.setProfilePhoto(profilePhoto);
        userToAdd.setImageUrl(resolveUserImageUrl(user.imageUrl(), profilePhoto));
        userToAdd.setWallet(wallet);
        userToAdd.setStatus(ACTIVE);

        userRepository.save(userToAdd);
    }

    @Transactional(readOnly = true)
    public List<ProfilePhotoDto> getAvailableProfilePhotos() {
        return profilePhotoRepository.findByEnabledTrueOrderByDisplayOrderAscIdAsc().stream()
                .map(photo -> new ProfilePhotoDto(photo.getId(), photo.getCode(), photo.getLabel(), photo.getImageUrl()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Long getDefaultProfilePhotoId() {
        return profilePhotoRepository.findFirstByEnabledTrueOrderByDisplayOrderAscIdAsc()
                .map(ProfilePhotoEntity::getId)
                .orElse(null);
    }

    @Transactional
    public void updateProfilePhoto(Long userId, Long profilePhotoId) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(profilePhotoId);
        var user = getByIdOrThrow(userId);
        var photo = profilePhotoRepository.findByIdAndEnabledTrue(profilePhotoId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid profile photo."));
        user.setProfilePhoto(photo);
        user.setImageUrl(photo.getImageUrl());
        userRepository.save(user);
    }

    @Transactional
    public void updateProfilePhotoByEmail(String email, Long profilePhotoId) {
        Objects.requireNonNull(email);
        Objects.requireNonNull(profilePhotoId);
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + email));
        updateProfilePhoto(user.getId(), profilePhotoId);
    }

    @Transactional(readOnly = true)
    public Optional<UserEntity> findByEmail(String email) {
        Objects.requireNonNull(email);
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public UserEntity getByIdOrThrow(Long id) {
        Objects.requireNonNull(id);
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }

    public List<UserDisplayDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserDisplayDto(user.getId(), user.getEmail(), user.getName(), user.getImageUrl(), user.getRole(), user.getStatus()))
                .toList();
    }

    public void banUser(Long bannedUserId, Long adminId, String reason, int duration) {
        Objects.requireNonNull(bannedUserId);
        Objects.requireNonNull(adminId);
        Objects.requireNonNull(reason);

        var user = getByIdOrThrow(bannedUserId);
        user.setStatus(BANNED);
        userRepository.save(user);

        var banHistory = new BanHistoryEntity();
        banHistory.setBannedUserId(bannedUserId);
        banHistory.setBannedById(adminId);
        banHistory.setReason(reason);
        banHistory.setDuration(duration);
        banHistory.setBanDate(LocalDateTime.now());
        banHistoryRepository.save(banHistory);
    }

    public void unban(Long id) {
        Objects.requireNonNull(id);
        var user = getByIdOrThrow(id);
        user.setStatus(ACTIVE);
        userRepository.save(user);
    }

    public List<BanHistoryDto> getBanHistory(Long id) {
        Objects.requireNonNull(id);
        var user = getByIdOrThrow(id);
        return banHistoryRepository.findByBannedUserId(user.getId()).stream()
                .map(ban -> new BanHistoryDto(
                        ban.getId(),
                        ban.getBannedUserId(),
                        ban.getBannedById(),
                        ban.getBanDate(),
                        ban.getReason(),
                        ban.getDuration()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal getWalletBalance(String email) {
        Objects.requireNonNull(email);
        var user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            throw new UserNotFoundException("User not found: " + email);
        }
        var wallet = user.get().getWallet();
        if (wallet == null) {
            throw new IllegalStateException("User has no wallet: " + email);
        }
        return wallet.getMoney();
    }

    private ProfilePhotoEntity resolveProfilePhotoForRegistration(Long profilePhotoId) {
        if (profilePhotoId != null) {
            return profilePhotoRepository.findByIdAndEnabledTrue(profilePhotoId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid profile photo."));
        }
        return profilePhotoRepository.findFirstByEnabledTrueOrderByDisplayOrderAscIdAsc().orElse(null);
    }

    private String resolveUserImageUrl(String imageUrl, ProfilePhotoEntity profilePhoto) {
        if (profilePhoto != null) {
            return profilePhoto.getImageUrl();
        }
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        return imageUrl.trim();
    }
}

