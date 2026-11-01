package fr.uge.parking.france.service;

import fr.uge.parking.france.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class PredictionService {
  private static final Random RANDOM = new Random();


  /**
   * Cette méthode simule une interaction avec un moteur de prédiction qui donne une estimation de la disponibilité d'une liste de places.
   * @param spaces
   * @return
   */
  private Availability getAvailabilities(List<Space> spaces){
    int capacity = spaces.size();
    int rand = RANDOM.nextInt(capacity);
    log.info("getAvailabilities prediction capacity: {}, available: {}", capacity, rand);
    return new Availability(rand, capacity - rand, capacity);
  }

  /**
   * Cette méthode simule un moteur de prédiction qui donne a dune date précise la disponibilité d'une liste de parkings.
   * @param parkingArround
   * @return
   */
  public List<ParkingAvailabilityPrediction> getAvailabilitiesPrediction(List<Parking> parkingArround, PredictionRequestArounds predictionRequestArounds) {
    return parkingArround.stream().map(p -> {

      final var availability = getAvailabilities(p.getInformation().getPlaces().stream()
              .filter(space -> space.getType().equals(predictionRequestArounds.getUserPosition().type())).toList());

      return new ParkingAvailabilityPrediction(new ParkingPrediction(p.getId(), p.getName(), p.getAddress()),availability.available(), availability.occupied(), availability.occupied());
    }).toList();
  }
}
