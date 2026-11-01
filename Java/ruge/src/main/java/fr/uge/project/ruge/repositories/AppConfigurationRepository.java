package fr.uge.project.ruge.repositories;

import fr.uge.project.ruge.entity.AppConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppConfigurationRepository extends JpaRepository<AppConfigurationEntity, Integer> {
}