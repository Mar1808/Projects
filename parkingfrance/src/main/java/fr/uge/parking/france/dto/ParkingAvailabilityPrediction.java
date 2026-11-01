package fr.uge.parking.france.dto;

public record ParkingAvailabilityPrediction(ParkingPrediction parkingPrediction, int available, int occupied, int capacity) {
}
