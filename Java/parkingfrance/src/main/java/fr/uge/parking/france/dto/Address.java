package fr.uge.parking.france.dto;


import jakarta.persistence.*;
import lombok.ToString;
import lombok.Getter;

@Entity
@ToString(exclude = "id")
public class Address {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Getter
  private String street;
  @Getter
  private long postalCode;
  @Getter
  private String city;
  @Getter
  private String country;

  @Getter
  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "position_id")
  private Position position;

  public Address() {
  }
  public Address(String street, long postalCode, String city, String country, Position position) {
    this.street = street;
    this.postalCode = postalCode;
    this.city = city;
    this.country = country;
    this.position = position;
  }

}
