package fr.uge.parking.france.service;

import fr.uge.parking.france.dto.*;
import fr.uge.parking.france.repositories.ParkingRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {

  private final ParkingRepository parkingRepository;
  private final SpaceService spaceService;
  private final PredictionService predictionService;



  // retourne la liste des parkings autour de la position de l'utilisateur
  public List<ParkingAvailability> findParkingsAround(UserPosition userPosition) {
    final var parkingList = getAllParkingsArround(userPosition);

    return parkingList.stream().map(p -> {
      final var availability = spaceService.getAvailabilities(p.getInformation().getPlaces().stream()
              .filter(space -> space.getType().equals(userPosition.type())).toList());
      return new ParkingAvailability(p, availability.available(), availability.occupied(), availability.capacity());
    }).toList();
  }

  private List<Parking> getAllParkingsArround(UserPosition userPosition) {
    var parkings = parkingRepository.findAll();
    List<Parking> parkingList = parkings.stream()
            .filter(parking -> parking
                    .getAddress()
                    .getPosition()
                    .distanceTo(userPosition.position()) <= userPosition.radius()).toList();
    return parkingList;
  }

  public List<Parking> findParkingsByCity(String city) {
    return parkingRepository.findByAddress_City(city);
  }

  public List<Equipment> loadParkingEquipments(long id) {
    log.info("Finding parking equipments by id: {}", id);
    return parkingRepository.findById(id).getInformation().getEquipments();
  }


  public List<ParkingAvailabilityPrediction> loadPredictionArround(PredictionRequestArounds predictionRequestArounds) {
    final var parkingArround = getAllParkingsArround(predictionRequestArounds.getUserPosition());

    return predictionService.getAvailabilitiesPrediction(parkingArround, predictionRequestArounds);
  }
}
