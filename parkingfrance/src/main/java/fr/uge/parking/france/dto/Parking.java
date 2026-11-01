package fr.uge.parking.france.dto;

import jakarta.persistence.*;
import lombok.ToString;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Entity
@ToString(exclude = "id")
public class Parking {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;
  private String name;

  @OneToOne(optional = false, cascade = CascadeType.ALL)
  @JoinColumn(name = "address_id", nullable = false)
  private Address address;

  @Setter
  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "information_id")
  private ParkingInfo information;

  public Parking(String name, Address address, ParkingInfo information) {
    this.name = name;
    this.address = Objects.requireNonNull(address);
    this.information = Objects.requireNonNull(information);
  }

  public Parking() {

  }

  public void renameTo(String newName) {
    this.name = Objects.requireNonNull(newName);
  }

  public void updateAddress(Address newAddress) {
    Objects.requireNonNull(newAddress);
    if(newAddress.getCity() == null || newAddress.getStreet() == null){
      throw new IllegalStateException("Invalid address");
    }
    this.address = newAddress;
  }


}
