package fr.uge.parking.france.dto;

import jakarta.persistence.*;
import lombok.ToString;
import lombok.Getter;

import java.util.Objects;

@Getter
@Entity
@ToString(exclude = "id")
public class Space {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;
  private ParkingSpaceType type;
  private boolean tempClosedSpace;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "position_id")
  private Position position;

  public Space() {
  }

  public Space(ParkingSpaceType type, boolean tempClosedSpace, Position position) {
    this.type = type;
    this.tempClosedSpace = tempClosedSpace;
    this.position = position;
  }

  public void changeTypeTo(ParkingSpaceType newType) {
    Objects.requireNonNull(newType);
    if (tempClosedSpace) {
      throw new IllegalStateException("Cannot change type of a closed space.");
    }
    this.type = newType;
  }

}
