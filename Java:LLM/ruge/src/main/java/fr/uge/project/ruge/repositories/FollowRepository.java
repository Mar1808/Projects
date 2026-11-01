package fr.uge.project.ruge.repositories;

import fr.uge.project.ruge.entity.FollowEntity;
import fr.uge.project.ruge.entity.FollowIdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowRepository extends JpaRepository<FollowEntity, FollowIdEntity> {
  boolean existsByFollowerIdAndFollowed_Id(Long followerId, Long followedId);

  void deleteByFollowerIdAndFollowed_Id(Long followerId, Long followedId);
}

