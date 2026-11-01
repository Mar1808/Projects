package fr.uge.parking.france.dto;


import lombok.Getter;

@Getter
public class PredictionRequestArounds {

  private UserPosition userPosition;

  private String date;
}
