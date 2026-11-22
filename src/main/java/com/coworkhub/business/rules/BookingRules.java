package com.coworkhub.business.rules;

import com.coworkhub.domain.Booking;
import com.coworkhub.domain.Location;
import com.coworkhub.domain.Member;
import com.coworkhub.domain.MembershipPlan;
import com.coworkhub.domain.Room;
import com.coworkhub.domain.enums.BookingStatus;
import com.coworkhub.exception.BusinessRuleException;
import com.coworkhub.exception.ErrorCodes;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Reglas de negocio de reservas expresadas como funciones puras (fáciles de probar).
 * Cada método lanza {@link BusinessRuleException} con el código de la regla incumplida.
 */
@Component
public class BookingRules {

    public static final int MIN_MINUTES = 30;
    public static final int MAX_MINUTES = 8 * 60;
    public static final int SLOT_MINUTES = 30;
    public static final Duration MIN_ADVANCE = Duration.ofHours(1);

    /** RN-02: entre 30 min y 8 h, en múltiplos de 30 min, inicio y fin el mismo día. */
    public void validateDuration(LocalDateTime start, LocalDateTime end) {
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            throw new BusinessRuleException(ErrorCodes.RN_02, "La reserva debe comenzar y terminar el mismo día");
        }
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < MIN_MINUTES || minutes > MAX_MINUTES) {
            throw new BusinessRuleException(ErrorCodes.RN_02,
                    "La reserva debe durar entre 30 minutos y 8 horas (duración solicitada: " + minutes + " min)");
        }
        if (minutes % SLOT_MINUTES != 0) {
            throw new BusinessRuleException(ErrorCodes.RN_02,
                    "La duración debe ser múltiplo de 30 minutos (duración solicitada: " + minutes + " min)");
        }
    }

    /** RN-03: la reserva debe ser futura y hacerse con al menos 1 hora de anticipación. */
    public void validateAdvance(LocalDateTime start, LocalDateTime now) {
        if (!start.isAfter(now)) {
            throw new BusinessRuleException(ErrorCodes.RN_03, "La reserva debe ser en una fecha y hora futura");
        }
        if (start.isBefore(now.plus(MIN_ADVANCE))) {
            throw new BusinessRuleException(ErrorCodes.RN_03,
                    "La reserva debe realizarse con al menos una hora de anticipación");
        }
    }

    /** RN-03: la reserva debe estar dentro del horario de la sede. */
    public void validateWithinOpeningHours(Location location, LocalTime start, LocalTime end) {
        if (start.isBefore(location.getOpeningTime()) || end.isAfter(location.getClosingTime())) {
            throw new BusinessRuleException(ErrorCodes.RN_03,
                    "El horario solicitado está fuera del horario de la sede (" + location.getOpeningTime()
                            + " - " + location.getClosingTime() + ")");
        }
    }

    /** RN-04: sala activa y con capacidad suficiente. */
    public void validateRoom(Room room, int attendees) {
        if (!room.isActive()) {
            throw new BusinessRuleException(ErrorCodes.RN_04, "La sala no está activa");
        }
        if (attendees > room.getCapacity()) {
            throw new BusinessRuleException(ErrorCodes.RN_04,
                    "La sala admite máximo " + room.getCapacity() + " asistentes (solicitados: " + attendees + ")");
        }
    }

    /** RN-05: solo miembros ACTIVE pueden reservar. */
    public void validateMemberActive(Member member) {
        if (!member.isActive()) {
            throw new BusinessRuleException(ErrorCodes.RN_05,
                    "El miembro no está activo (estado: " + member.getStatus() + ") y no puede reservar");
        }
    }

    /** RN-06: no superar el máximo de reservas activas futuras del plan. */
    public void validateActiveBookingLimit(long activeFutureBookings, MembershipPlan plan) {
        if (activeFutureBookings >= plan.getMaxActiveBookings()) {
            throw new BusinessRuleException(ErrorCodes.RN_06,
                    "El miembro alcanzó el máximo de " + plan.getMaxActiveBookings()
                            + " reservas activas futuras permitido por su plan " + plan.getName());
        }
    }

    /**
     * RN-01: no puede haber otra reserva PENDING/CONFIRMED que se cruce.
     * Dos rangos [a,b) y [c,d) se cruzan si a &lt; d y c &lt; b. Si una termina exactamente cuando
     * empieza la otra, NO se cruzan.
     */
    public void validateNoOverlap(List<Booking> candidates, LocalDateTime start, LocalDateTime end) {
        boolean overlap = candidates.stream()
                .filter(b -> BookingStatus.ACTIVE.contains(b.getStatus()))
                .anyMatch(b -> overlaps(b.getStartTime(), b.getEndTime(), start, end));
        if (overlap) {
            throw new BusinessRuleException(ErrorCodes.RN_01,
                    "La sala ya tiene una reserva que se cruza con el horario solicitado");
        }
    }

    public static boolean overlaps(LocalDateTime aStart, LocalDateTime aEnd, LocalDateTime bStart, LocalDateTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    /** RN-09: transiciones de estado permitidas. */
    public void validateTransition(BookingStatus current, BookingStatus target) {
        if (!current.canTransitionTo(target)) {
            throw new BusinessRuleException(ErrorCodes.RN_09,
                    "No se permite cambiar una reserva de " + current + " a " + target);
        }
    }

    /** RN-10: solo se modifican los servicios de reservas PENDING. */
    public void validateServicesEditable(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessRuleException(ErrorCodes.RN_10,
                    "Solo se pueden modificar los servicios de reservas PENDING (estado actual: "
                            + booking.getStatus() + ")");
        }
    }
}
