package fr.uge.parking.france.dto;

public record ErrorResponse(int code, String message) {
  @Override
  public String toString() {
    return """
        {
          "code": %d,
          "message": "%s"
        }
        """.formatted(code, message);
  }
}
