package fr.uge.parking.france.dto;

import jakarta.persistence.*;
import lombok.ToString;
import lombok.Getter;
import lombok.Setter;


import java.util.List;
import java.util.Objects;

@Entity
@ToString
public class ParkingInfo {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Getter
  private int price;

  @Setter
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "space_id")
  private List<Space> places;

  @Getter
  @ElementCollection(targetClass = Equipment.class)
  @Enumerated(EnumType.STRING)
  private List<Equipment> equipments;

  public ParkingInfo() {
  }

  public ParkingInfo(int price, List<Space> places, List<Equipment> equipments) {
    this.price = price;
    this.places = places;
    this.equipments = equipments;
  }

  public List<Space> getPlaces() {
    return List.copyOf(places);
  }

  public ParkingInfo withEquipments(List<Equipment> newEquipments) {
    Objects.requireNonNull(newEquipments);
    return new ParkingInfo(this.price, this.places, newEquipments);
  }

  public ParkingInfo withPrice(int newPrice) {
    Objects.requireNonNull(newPrice);
    return new ParkingInfo(newPrice, this.places, this.equipments);
  }
}
