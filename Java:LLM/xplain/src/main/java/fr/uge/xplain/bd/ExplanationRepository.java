package fr.uge.xplain.bd;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExplanationRepository extends JpaRepository<Explanation, Long> {
    List<Explanation> findByOrderByIdDesc();
}
