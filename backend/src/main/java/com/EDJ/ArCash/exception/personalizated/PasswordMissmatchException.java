package com.EDJ.ArCash.exception.personalizated;

public class PasswordMissmatchException extends RuntimeException {
  public PasswordMissmatchException(String message) {
    super(message);
  }
}
