package com.EDJ.ArCash.Service.interfaces;

import com.EDJ.ArCash.Models.CardPin;
import com.EDJ.ArCash.Models.User;

public interface CardPinService {
    public boolean isConfigured(Long userId);

    public boolean isValidFormat(String pin);

    public PinResult setPin(User user, String pin, String confirm, String currentPin);

    public PinResult verify(User user, String pin);

    public record PinResult(boolean success, String message, String unlockToken, boolean locked) {
            public static PinResult ok(String message, String token) {
                return new PinResult(true, message, token, false);
            }

            public static PinResult invalid(String message) {
                return new PinResult(false, message, null, false);
            }

            public static PinResult locked(String message) {
                return new PinResult(false, message, null, true);
            }
        }

}
