package fr.uge.parking.france.service;

import fr.uge.parking.france.dto.Availability;
import fr.uge.parking.france.dto.Space;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class SpaceService {

  private static final Random RANDOM = new Random();

  /**
   * Cette méthode simule une interaction avec des capteurs qui donne en temps réel la disponibilité d"une place.
   * @param space une place de parking
   * @return true si la place est disponible false sinon
   */
  public boolean isSpaceAvailable(Space space){
    final var b = RANDOM.nextBoolean();
    log.info("isSpaceAvailable on spaceId: {}, available : {}", space.getId(), b);
    return b;
  }

  /**
   * Cette méthode simule une interaction avec des capteurs qui donne en temps réel la disponibilité d'une liste de places.
    * @param spaces
   * @return
   */
  public Availability getAvailabilities(List<Space> spaces){
    int capacity = spaces.size();
    int rand = RANDOM.nextInt(capacity);
    log.info("getAvailabilities capacity: {}, available: {}", capacity, rand);
    return new Availability(rand, capacity - rand, capacity);
  }
}
