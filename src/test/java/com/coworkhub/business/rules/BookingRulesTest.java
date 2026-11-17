package com.coworkhub.business.rules;

import com.coworkhub.TestData;
import com.coworkhub.domain.Booking;
import com.coworkhub.domain.Room;
import com.coworkhub.domain.enums.BookingStatus;
import com.coworkhub.domain.enums.MemberStatus;
import com.coworkhub.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingRulesTest {

    private final BookingRules rules = new BookingRules();
    private final LocalDateTime day = LocalDateTime.of(2030, 1, 15, 0, 0);

    // ---------------- RN-02 Duración ----------------

    @ParameterizedTest(name = "{0} minutos es válido")
    @CsvSource({"30", "60", "90", "480"})
    void validDurations(int minutes) {
        LocalDateTime start = day.withHour(9);
        assertThatCode(() -> rules.validateDuration(start, start.plusMinutes(minutes))).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0} minutos es inválido")
    @CsvSource({"15", "45", "510", "540"})
    void invalidDurations(int minutes) {
        LocalDateTime start = day.withHour(9);
        assertThatThrownBy(() -> rules.validateDuration(start, start.plusMinutes(minutes)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("RN-02");
    }

    @Test
    @DisplayName("RN-02: la reserva debe terminar el mismo día")
    void mustEndSameDay() {
        LocalDateTime start = day.withHour(23);
        assertThatThrownBy(() -> rules.validateDuration(start, start.plusHours(2)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("RN-02");
    }

    // ---------------- RN-03 Horarios ----------------

    @Test
    @DisplayName("RN-03: no se permite reservar en el pasado")
    void rejectsPast() {
        LocalDateTime now = day.withHour(10);
        assertThatThrownBy(() -> rules.validateAdvance(now.minusHours(1), now))
                .extracting("code").isEqualTo("RN-03");
    }

    @Test
    @DisplayName("RN-03: exige al menos una hora de anticipación")
    void requiresOneHourAdvance() {
        LocalDateTime now = day.withHour(10);
        assertThatThrownBy(() -> rules.validateAdvance(now.plusMinutes(59), now))
                .extracting("code").isEqualTo("RN-03");
        assertThatCode(() -> rules.validateAdvance(now.plusHours(1), now)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("RN-03: debe estar dentro del horario de la sede (07:00-20:00)")
    void withinOpeningHours() {
        var location = TestData.location();
        assertThatCode(() -> rules.validateWithinOpeningHours(location, LocalTime.of(7, 0), LocalTime.of(20, 0)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> rules.validateWithinOpeningHours(location, LocalTime.of(6, 30), LocalTime.of(8, 0)))
                .extracting("code").isEqualTo("RN-03");
        assertThatThrownBy(() -> rules.validateWithinOpeningHours(location, LocalTime.of(19, 0), LocalTime.of(20, 30)))
                .extracting("code").isEqualTo("RN-03");
    }

    // ---------------- RN-04 / RN-05 / RN-06 ----------------

    @Test
    @DisplayName("RN-04: sala inactiva o capacidad insuficiente")
    void roomRules() {
        Room room = TestData.room();
        assertThatThrownBy(() -> rules.validateRoom(room, 9)).extracting("code").isEqualTo("RN-04");
        room.setActive(false);
        assertThatThrownBy(() -> rules.validateRoom(room, 2)).extracting("code").isEqualTo("RN-04");
    }

    @Test
    @DisplayName("RN-05: un miembro suspendido no puede reservar")
    void suspendedMemberRejected() {
        assertThatThrownBy(() -> rules.validateMemberActive(TestData.member(1L, MemberStatus.SUSPENDED)))
                .extracting("code").isEqualTo("RN-05");
    }

    @Test
    @DisplayName("RN-06: límite de reservas activas futuras del plan")
    void activeBookingLimit() {
        var plan = TestData.plan(20, "0.15", 2);
        assertThatCode(() -> rules.validateActiveBookingLimit(1, plan)).doesNotThrowAnyException();
        assertThatThrownBy(() -> rules.validateActiveBookingLimit(2, plan)).extracting("code").isEqualTo("RN-06");
    }

    // ---------------- RN-01 Cruces ----------------

    @Test
    @DisplayName("RN-01: reservas cruzadas se rechazan")
    void overlappingRejected() {
        Booking existing = TestData.booking(1L, TestData.member(1L, MemberStatus.ACTIVE),
                day.withHour(9), day.withHour(10), BookingStatus.CONFIRMED);
        assertThatThrownBy(() -> rules.validateNoOverlap(List.of(existing), day.withHour(9).plusMinutes(30),
                day.withHour(10).plusMinutes(30)))
                .extracting("code").isEqualTo("RN-01");
    }

    @Test
    @DisplayName("RN-01: reservas consecutivas (09-10 y 10-11) son válidas")
    void consecutiveAllowed() {
        Booking existing = TestData.booking(1L, TestData.member(1L, MemberStatus.ACTIVE),
                day.withHour(9), day.withHour(10), BookingStatus.PENDING);
        assertThatCode(() -> rules.validateNoOverlap(List.of(existing), day.withHour(10), day.withHour(11)))
                .doesNotThrowAnyException();
        assertThat(BookingRules.overlaps(day.withHour(9), day.withHour(10), day.withHour(10), day.withHour(11))).isFalse();
    }

    @Test
    @DisplayName("RN-01: una reserva CANCELLED no bloquea la sala")
    void cancelledDoesNotBlock() {
        Booking cancelled = TestData.booking(1L, TestData.member(1L, MemberStatus.ACTIVE),
                day.withHour(9), day.withHour(10), BookingStatus.CANCELLED);
        assertThatCode(() -> rules.validateNoOverlap(List.of(cancelled), day.withHour(9), day.withHour(10)))
                .doesNotThrowAnyException();
    }

    // ---------------- RN-09 Transiciones ----------------

    @ParameterizedTest(name = "{0} -> {1} permitido")
    @CsvSource({"PENDING,CONFIRMED", "CONFIRMED,COMPLETED", "PENDING,CANCELLED", "CONFIRMED,CANCELLED"})
    void allowedTransitions(BookingStatus from, BookingStatus to) {
        assertThatCode(() -> rules.validateTransition(from, to)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0} -> {1} prohibido")
    @CsvSource({"CANCELLED,CONFIRMED", "COMPLETED,CANCELLED", "PENDING,COMPLETED", "CANCELLED,COMPLETED", "COMPLETED,CONFIRMED"})
    void forbiddenTransitions(BookingStatus from, BookingStatus to) {
        assertThatThrownBy(() -> rules.validateTransition(from, to)).extracting("code").isEqualTo("RN-09");
    }
}
