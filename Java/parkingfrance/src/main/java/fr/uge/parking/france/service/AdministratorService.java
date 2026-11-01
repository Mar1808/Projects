package fr.uge.parking.france.service;

import fr.uge.parking.france.dto.*;
import fr.uge.parking.france.repositories.ParkingRepository;
import fr.uge.parking.france.repositories.TopologyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class AdministratorService {

  private final ParkingRepository parkingRepository;
  private final TopologyRepository topologyRepository;

  public AdministratorService(ParkingRepository parkingRepository, TopologyRepository topologyRepository) {
    this.topologyRepository = topologyRepository;
    this.parkingRepository = parkingRepository;
  }

  public void createParking(Parking parking) {
    log.info("Creating parking: {}", parking);
    parkingRepository.save(parking);
  }

  public void updateParking(Parking parking) {
    log.info("Updating parking: {}", parking);
    if (!parkingRepository.existsById(parking.getId())) {
      log.warn("Attempted to update non-existing parking with id {}", parking.getId());
      throw new IllegalArgumentException("Cannot update non-existing parking with id " + parking.getId());
    }
    parkingRepository.save(parking);
    log.info("Parking {} updated successfully", parking.getId());
  }

  public Optional<Parking> getParkingById(long id) {
    log.info("Fetching parking with id {}", id);
    return Optional.ofNullable(parkingRepository.findById(id));
  }

  public void deleteParkingById(long id) {
    log.info("Deleting parking with id {}", id);
    parkingRepository.deleteById(id);
  }

  public List<Parking> getAllParkings() {
    log.info("Fetching all parkings");
    return parkingRepository.findAll();
  }

  public void createTopology(Topology topology) {
    log.info("Creating topology: {}", topology);
    topologyRepository.save(topology);
  }

  // deprecated

  public Parking replaceDeprecatedParking(long id, Parking parking) {
    log.warn("Deprecated PATCH /admin/v1/parkings/{id} called for {}", id);

    if (getParkingById(id).isEmpty()) {
      log.warn("Parking not found for id {}", id);
      throw new IllegalArgumentException("Parking not found");
    }

    deleteParkingById(id);
    createParking(parking);
    log.info("Parking {} replaced via deprecated endpoint", id);
    return parking;
  }

  public void updateParkingEquipmentsV1(long id, ParkingInfo info) {
    log.info("Updating equipments (v1) for parking id {}", id);
    var parking = getParkingById(id)
        .orElseThrow(() -> new RuntimeException("Parking not found"));
    parking.setInformation(info);
    updateParking(parking);
    log.info("Equipments updated successfully for parking {}", id);
  }

  public void updateParkingTypesV1(long id, ParkingInfo info) {
    log.info("Updating types (v1) for parking id {}", id);
    var parking = getParkingById(id)
        .orElseThrow(() -> new RuntimeException("Parking not found"));
    parking.setInformation(info);
    updateParking(parking);
    log.info("Types updated successfully for parking {}", id);
  }

  // v2

  public ResponseEntity<Parking> updateParkingName(long id, String newName) {
    log.info("Updating name for parking id {}", id);
    var parkingOpt = getParkingById(id);
    if (parkingOpt.isEmpty()) {
      log.warn("Parking not found for id {}", id);
      return ResponseEntity.notFound().build();
    }

    try {
      var parking = parkingOpt.get();
      parking.renameTo(newName);
      updateParking(parking);
      log.info("Parking name updated successfully for id {}", id);
      return ResponseEntity.ok(parking);
    } catch (IllegalArgumentException | NullPointerException e) {
      log.warn("Invalid name update for parking {}: {}", id, e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  public ResponseEntity<Parking> updateParkingAddress(long id, Address newAddress) {
    log.info("Updating address for parking id {}", id);
    var parkingOpt = getParkingById(id);
    if (parkingOpt.isEmpty()) {
      log.warn("Parking not found for id {}", id);
      return ResponseEntity.notFound().build();
    }

    try {
      var parking = parkingOpt.get();
      parking.updateAddress(newAddress);
      updateParking(parking);
      log.info("Address updated successfully for parking id {}", id);
      return ResponseEntity.ok(parking);
    } catch (IllegalStateException | NullPointerException e) {
      log.warn("Invalid address update for parking {}: {}", id, e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  public ResponseEntity<Parking> updateParkingPrice(long id, int newPrice) {
    log.info("Updating price for parking id {}", id);
    var parkingOpt = getParkingById(id);
    if (parkingOpt.isEmpty()) {
      log.warn("Parking not found for id {}", id);
      return ResponseEntity.notFound().build();
    }

    var parking = parkingOpt.get();
    var info = parking.getInformation();
    if (info == null) {
      log.warn("ParkingInfo is null for parking {}", id);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    parking.setInformation(info.withPrice(newPrice));
    updateParking(parking);
    log.info("Price updated successfully for parking id {}", id);
    return ResponseEntity.ok(parking);
  }

  public ResponseEntity<Parking> updateParkingEquipments(long id, EquipmentRequest request) {
    log.info("Updating equipments (v2) for parking id {}", id);
    var parkingOpt = getParkingById(id);
    if (parkingOpt.isEmpty()) {
      log.warn("Parking not found for id {}", id);
      return ResponseEntity.notFound().build();
    }

    var parking = parkingOpt.get();
    var info = parking.getInformation();
    if (info == null) {
      log.warn("ParkingInfo is null for parking {}", id);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    parking.setInformation(info.withEquipments(request.getEquipments()));
    updateParking(parking);
    log.info("Equipments updated successfully for parking id {}", id);
    return ResponseEntity.ok(parking);
  }

  public ResponseEntity<Parking> updateParkingSpaceType(long id, long spaceId, ParkingSpaceType newType) {
    log.info("Updating space type for parking {} space {}", id, spaceId);
    var parkingOpt = getParkingById(id);
    if (parkingOpt.isEmpty()) {
      log.warn("Parking not found for id {}", id);
      return ResponseEntity.notFound().build();
    }

    var parking = parkingOpt.get();
    var info = parking.getInformation();
    if (info == null) {
      log.warn("ParkingInfo is null for parking {}", id);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    var spaceOpt = info.getPlaces().stream()
        .filter(s -> s.getId() == spaceId)
        .findFirst();

    if (spaceOpt.isEmpty()) {
      log.warn("Space {} not found in parking {}", spaceId, id);
      return ResponseEntity.notFound().build();
    }

    spaceOpt.get().changeTypeTo(newType);
    updateParking(parking);
    log.info("Space type updated successfully for space {} in parking {}", spaceId, id);
    return ResponseEntity.ok(parking);
  }

  public ResponseEntity<Parking> updateParkingSpaceArea(long id, long spaceId, Topology newTopology) {
    log.info("Updating area for space {} in parking {}", spaceId, id);
    var parkingOpt = getParkingById(id);
    if (parkingOpt.isEmpty()) {
      log.warn("Parking not found for id {}", id);
      return ResponseEntity.notFound().build();
    }

    createTopology(newTopology);
    log.info("Area updated successfully for space {} in parking {}", spaceId, id);
    return ResponseEntity.ok(parkingOpt.get());
  }

}
