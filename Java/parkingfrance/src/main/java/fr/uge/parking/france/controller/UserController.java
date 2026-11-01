package fr.uge.parking.france.controller;

import fr.uge.parking.france.dto.*;
import fr.uge.parking.france.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/parkings/v1")
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/arounds")
  public List<ParkingAvailability> getParkingsAround(@RequestBody UserPosition userPosition) {
    return userService.findParkingsAround(userPosition);
  }

  @PostMapping("/arounds/prediction")
  public List<ParkingAvailabilityPrediction> getParkingsAroundWithPrediction(@RequestBody PredictionRequestArounds predictionRequestArounds) {
  return userService.loadPredictionArround(predictionRequestArounds);
  }

  @GetMapping("/city/{city}")
  public List<Parking> getParkingsByCity(@PathVariable String city) {
    return userService.findParkingsByCity(city);
  }

  @GetMapping("/{id}/availability")
  public void getParkingAvailability(@PathVariable long id, @RequestParam ParkingSpaceType spaceType) {
    // TODO: implement availability logic
  }

  @GetMapping("/{id}/equipments")
  public List<Equipment> getParkingEquipments(@PathVariable long id) {
    return userService.loadParkingEquipments(id);
  }

  @PostMapping("/{id}/prediction")
  public void getParkingPrediction(@PathVariable long id) {
    // TODO: implement prediction logic
  }
}
