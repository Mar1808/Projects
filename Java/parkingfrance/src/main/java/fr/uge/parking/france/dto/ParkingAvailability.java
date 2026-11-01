package fr.uge.parking.france.dto;

public record ParkingAvailability(Parking parking, int available, int occupied, int capacity) {
}
