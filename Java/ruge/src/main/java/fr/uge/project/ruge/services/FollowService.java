package fr.uge.project.ruge.services;

import fr.uge.project.ruge.entity.FollowEntity;
import fr.uge.project.ruge.entity.FollowIdEntity;
import fr.uge.project.ruge.exceptions.UserNotFoundException;
import fr.uge.project.ruge.repositories.FollowRepository;
import fr.uge.project.ruge.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Service
public class FollowService {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public FollowService(FollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = Objects.requireNonNull(followRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(Long followerId, Long followedId) {
        if (followerId == null || followedId == null) {
            return false;
        }
        return followRepository.existsByFollowerIdAndFollowed_Id(followerId, followedId);
    }

    @Transactional
    public void follow(Long followerId, Long followedId) {
        if (followerId == null || followedId == null) {
            throw new IllegalArgumentException("Invalid user.");
        }
        if (Objects.equals(followerId, followedId)) {
            throw new IllegalArgumentException("You cannot follow yourself.");
        }
        if (followRepository.existsByFollowerIdAndFollowed_Id(followerId, followedId)) {
            return;
        }

        var follower = userRepository.findById(followerId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + followerId));
        var followed = userRepository.findById(followedId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + followedId));

        var follow = new FollowEntity();
        var id = new FollowIdEntity();
        id.setFollowerId(followerId);
        id.setFollowedId(followedId);
        follow.setId(id);
        follow.setFollower(follower);
        follow.setFollowed(followed);
        follow.setDateFollow(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        followRepository.save(follow);
    }

    @Transactional
    public void unfollow(Long followerId, Long followedId) {
        if (followerId == null || followedId == null) {
            return;
        }
        followRepository.deleteByFollowerIdAndFollowed_Id(followerId, followedId);
    }
}

