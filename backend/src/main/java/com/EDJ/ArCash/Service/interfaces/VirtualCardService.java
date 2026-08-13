package com.EDJ.ArCash.Service.interfaces;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Imp.CardStatus;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Models.VirtualCard;
import java.util.List;
import java.util.Optional;

public interface VirtualCardService {
    int REISSUE_COOLDOWN_DAYS = 7;

    VirtualCard createForAccount(Account account);

    List<VirtualCard> listOrBackfillForUser(User user);

    Optional<VirtualCard> findOwned(Long cardId, Long userId);

    VirtualCard updateStatus(VirtualCard card, CardStatus status);

    VirtualCard updateLimit(VirtualCard card, double dailyLimit);

    VirtualCard cancel(VirtualCard card);

    VirtualCard reissue(VirtualCard card);

    boolean isExpired(VirtualCard card);

    ReissueEligibility reissueEligibility(VirtualCard card);

    String decryptPan(VirtualCard card);

    String decryptCvc(VirtualCard card);

    static String formatPan(String pan) {
        String digits = pan.replaceAll("\\D", "");
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (int i = 0; i < digits.length(); i += 4) {
            parts.add(digits.substring(i, Math.min(i + 4, digits.length())));
        }
        return String.join(" ", parts);
    }

    record ReissueEligibility(boolean allowed, String message) {
        public static ReissueEligibility ok(String message) {
            return new ReissueEligibility(true, message);
        }

        public static ReissueEligibility blocked(String message) {
            return new ReissueEligibility(false, message);
        }
    }
}
