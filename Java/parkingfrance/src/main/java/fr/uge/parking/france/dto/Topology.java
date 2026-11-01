package fr.uge.parking.france.dto;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.Objects;

@Entity
public class Topology {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Getter
  private String zone;
  @Getter
  private int niveau;

  @Getter
  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "position_id")
  private Position position;

  public Topology() {
  }

  public Topology(String zone, int niveau, Position position) {
    this.zone = Objects.requireNonNull(zone);
    this.niveau = niveau;
    this.position = Objects.requireNonNull(position);
  }

}
