package fr.uge.project.ruge.repositories;

import fr.uge.project.ruge.entity.ProfilePhotoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfilePhotoRepository extends JpaRepository<ProfilePhotoEntity, Long> {
  List<ProfilePhotoEntity> findByEnabledTrueOrderByDisplayOrderAscIdAsc();
  Optional<ProfilePhotoEntity> findByIdAndEnabledTrue(Long id);
  Optional<ProfilePhotoEntity> findFirstByEnabledTrueOrderByDisplayOrderAscIdAsc();
}
