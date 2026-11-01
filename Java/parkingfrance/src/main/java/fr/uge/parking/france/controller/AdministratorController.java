package fr.uge.parking.france.controller;

import fr.uge.parking.france.dto.*;
import fr.uge.parking.france.service.AdministratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin")
public class AdministratorController {

  private final AdministratorService administratorService;

  public AdministratorController(AdministratorService administratorService) {
    this.administratorService = administratorService;
  }
  //v1

  @GetMapping("/v1/parkings")
  public ResponseEntity<List<Parking>> getAllParkings() {
    try {
      return ResponseEntity.ok(administratorService.getAllParkings());
    } catch (Exception e) {
      log.error("Error retrieving parkings", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @PostMapping("/v1/parkings")
  public ResponseEntity<Void> createParking(@RequestBody Parking parking) {
    try {
      administratorService.createParking(parking);
      log.info("Parking created successfully: {}", parking);
      return ResponseEntity.status(HttpStatus.CREATED).build();
    } catch (Exception e) {
      log.error("Error creating parking", e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  @GetMapping("/v1/parkings/{id}")
  public ResponseEntity<Parking> getParkingById(@PathVariable long id) {
    return administratorService.getParkingById(id)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
  }

  @Deprecated
  @PatchMapping("/v1/parkings/{id}")
  public ResponseEntity<Parking> updateParking(@PathVariable long id, @RequestBody Parking parking) {
    try {
      var updated = administratorService.replaceDeprecatedParking(id, parking);
      return ResponseEntity.ok(updated);
    } catch (IllegalArgumentException e) {
      log.warn("Invalid data while updating parking {}: {}", id, e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("Unexpected error updating parking {}", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @DeleteMapping("/v1/parkings/{id}")
  public ResponseEntity<Void> deleteParkingById(@PathVariable long id) {
    try {
      administratorService.deleteParkingById(id);
      log.info("Parking deleted successfully: {}", id);
      return ResponseEntity.noContent().build();
    } catch (RuntimeException e) {
      log.warn("Parking not found for deletion: {}", id);
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    } catch (Exception e) {
      log.error("Error deleting parking {}", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @PutMapping("/v1/parkings/{id}/equipments")
  public ResponseEntity<Void> updateParkingEquipments(@PathVariable long id, @RequestBody ParkingInfo info) {
    try {
      administratorService.updateParkingEquipmentsV1(id, info);
      return ResponseEntity.ok().build();
    } catch (RuntimeException e) {
      log.warn("Parking not found for equipments update: {}", id);
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    } catch (Exception e) {
      log.error("Error updating equipments for parking {}", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @PutMapping("/v1/parkings/{id}/types")
  public ResponseEntity<Void> updateParkingTypes(@PathVariable long id, @RequestBody ParkingInfo info) {
    try {
      administratorService.updateParkingTypesV1(id, info);
      return ResponseEntity.ok().build();
    } catch (RuntimeException e) {
      log.warn("Parking not found for types update: {}", id);
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    } catch (Exception e) {
      log.error("Error updating types for parking {}", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @PutMapping("/v1/parkings/{id}/area")
  public ResponseEntity<Void> putArea(@PathVariable long id, @RequestBody Topology topology) {
    try {
      administratorService.createTopology(topology);
      return ResponseEntity.ok().build();
    } catch (Exception e) {
      log.error("Error updating area for parking {}", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }


  //v2

  @PatchMapping("/v2/parkings/{id}/name")
  public ResponseEntity<Parking> updateParkingName(@PathVariable long id, @RequestBody String newName) {
    return administratorService.updateParkingName(id, newName);
  }

  @PatchMapping("/v2/parkings/{id}/address")
  public ResponseEntity<Parking> updateParkingAddress(@PathVariable long id, @RequestBody Address newAddress) {
    return administratorService.updateParkingAddress(id, newAddress);
  }

  @PatchMapping("/v2/parkings/{id}/price")
  public ResponseEntity<Parking> updateParkingPrice(@PathVariable long id, @RequestBody int newPrice) {
    return administratorService.updateParkingPrice(id, newPrice);
  }

  @PatchMapping("/v2/parkings/{id}/equipments")
  public ResponseEntity<Parking> updateParkingEquipments(@PathVariable long id, @RequestBody EquipmentRequest request) {
    return administratorService.updateParkingEquipments(id, request);
  }

  @PatchMapping("/v2/parkings/{id}/space/{spaceId}/types")
  public ResponseEntity<Parking> updateParkingSpaceType(@PathVariable long id,
                                                        @PathVariable long spaceId,
                                                        @RequestBody ParkingSpaceType newType) {
    return administratorService.updateParkingSpaceType(id, spaceId, newType);
  }

  @PatchMapping("/v2/parkings/{id}/space/{spaceId}/area")
  public ResponseEntity<Parking> updateParkingSpaceArea(@PathVariable long id,
                                                        @PathVariable long spaceId,
                                                        @RequestBody Topology newTopology) {
    return administratorService.updateParkingSpaceArea(id, spaceId, newTopology);
  }

}
