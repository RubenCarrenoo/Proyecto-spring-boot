package com.coworkhub.domain.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados de una reserva y sus transiciones permitidas (RN-09):
 * <pre>
 * PENDING   -> CONFIRMED | CANCELLED
 * CONFIRMED -> COMPLETED | CANCELLED
 * COMPLETED -> (final)
 * CANCELLED -> (final)
 * </pre>
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED;

    /** Estados que ocupan la sala (RN-01) y cuentan como reservas activas (RN-06). */
    public static final Set<BookingStatus> ACTIVE = EnumSet.of(PENDING, CONFIRMED);

    public boolean canTransitionTo(BookingStatus target) {
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == COMPLETED || target == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
