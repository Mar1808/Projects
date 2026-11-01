package fr.uge.parking.france.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;

@Entity
public class Position {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Getter
  public double latitude;
  @Getter
  public double longitude;

  public Position() {
  }

  public Position(double latitude, double longitude) {
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public int distanceTo(Position position) {
    var latDiff = this.latitude - position.latitude;
    var lonDiff = this.longitude - position.longitude;
    return (int) Math.sqrt(latDiff * latDiff + lonDiff * lonDiff);
  }
}
