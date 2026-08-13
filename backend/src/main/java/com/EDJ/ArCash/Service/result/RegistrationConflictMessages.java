package com.EDJ.ArCash.Service.result;

import java.util.ArrayList;
import java.util.List;

public final class RegistrationConflictMessages {

    private RegistrationConflictMessages() {
    }

    public static String format(List<RegistrationConflictCode> codes) {
        List<String> fragments = new ArrayList<>();
        for (RegistrationConflictCode code : codes) {
            fragments.add(fragmentFor(code));
        }

        if (fragments.size() == 1) {
            String only = fragments.get(0);
            return Character.toUpperCase(only.charAt(0)) + only.substring(1) + ".";
        }

        if (fragments.size() == 2) {
            String first = fragments.get(0);
            return Character.toUpperCase(first.charAt(0)) + first.substring(1)
                    + " y " + fragments.get(1) + ".";
        }

        String last = fragments.remove(fragments.size() - 1);
        String first = fragments.get(0);
        StringBuilder combined = new StringBuilder();
        combined.append(Character.toUpperCase(first.charAt(0))).append(first.substring(1));
        for (int i = 1; i < fragments.size(); i++) {
            combined.append(", ").append(fragments.get(i));
        }
        combined.append(" y ").append(last).append(".");
        return combined.toString();
    }

    private static String fragmentFor(RegistrationConflictCode code) {
        return switch (code) {
            case EMAIL_ALREADY_EXISTS -> "el email ya se encuentra en uso";
            case ALIAS_ALREADY_EXISTS -> "el nombre de usuario no está disponible";
            case DNI_ALREADY_EXISTS -> "el DNI ya está registrado";
        };
    }
}
