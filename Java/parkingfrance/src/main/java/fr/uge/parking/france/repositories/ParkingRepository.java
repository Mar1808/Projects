package fr.uge.parking.france.repositories;

import fr.uge.parking.france.dto.Parking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingRepository extends JpaRepository<Parking, Long> {
  Parking findById(long id);
  List<Parking> findByAddress_City(String city);
}
