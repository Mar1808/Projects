package fr.uge.project.ruge.exceptions;

import java.util.Objects;

public class UserAlreadyExistsException extends RuntimeException {
  public UserAlreadyExistsException(String message) {
    super(Objects.requireNonNull(message));
  }
}
