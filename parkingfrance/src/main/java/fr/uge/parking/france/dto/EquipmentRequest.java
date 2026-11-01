package fr.uge.parking.france.dto;

import lombok.Getter;

import java.util.List;
import java.util.Objects;


@Getter
public class EquipmentRequest {

  private List<Equipment> equipments;

  public EquipmentRequest(List<Equipment> equipments) {
    this.equipments = Objects.requireNonNull(equipments);
  }
}