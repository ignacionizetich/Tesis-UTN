package com.EDJ.ArCash.Service;

import java.util.Collections;
import java.util.List;

/**
 * Conflictos de unicidad al registrar (email / alias / dni).
 */
public class RegistrationConflictException extends RuntimeException {

    private final List<RegistrationConflictCode> codes;

    public RegistrationConflictException(List<RegistrationConflictCode> codes) {
        super("Registration conflicts: " + codes);
        this.codes = List.copyOf(codes);
    }

    public List<RegistrationConflictCode> getCodes() {
        return Collections.unmodifiableList(codes);
    }
}
