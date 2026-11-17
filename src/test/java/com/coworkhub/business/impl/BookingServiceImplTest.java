package com.coworkhub.business.impl;

import com.coworkhub.TestData;
import com.coworkhub.business.rules.BookingRules;
import com.coworkhub.business.rules.CancellationPolicy;
import com.coworkhub.business.rules.CostCalculator;
import com.coworkhub.domain.Booking;
import com.coworkhub.domain.Member;
import com.coworkhub.domain.Room;
import com.coworkhub.domain.enums.BookingStatus;
import com.coworkhub.domain.enums.MemberStatus;
import com.coworkhub.dto.booking.BookingRequest;
import com.coworkhub.dto.booking.BookingResponse;
import com.coworkhub.dto.booking.BookingServiceRequest;
import com.coworkhub.exception.BusinessRuleException;
import com.coworkhub.mapper.BookingMapper;
import com.coworkhub.persistence.AdditionalServiceRepository;
import com.coworkhub.persistence.BookingRepository;
import com.coworkhub.persistence.MemberRepository;
import com.coworkhub.persistence.RoomRepository;
import com.coworkhub.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");
    /** "Ahora" fijo: 15/01/2030 08:00. */
    private static final LocalDateTime NOW = LocalDateTime.of(2030, 1, 15, 8, 0);

    @Mock BookingRepository bookingRepository;
    @Mock RoomRepository roomRepository;
    @Mock MemberRepository memberRepository;
    @Mock AdditionalServiceRepository serviceRepository;
    @Mock CurrentUserProvider currentUser;

    private BookingServiceImpl service;
    private Member member;
    private Room room;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        service = new BookingServiceImpl(bookingRepository, roomRepository, memberRepository, serviceRepository,
                new BookingRules(), new CostCalculator(), new CancellationPolicy(), currentUser,
                new BookingMapper(), clock);

        member = TestData.member(1L, MemberStatus.ACTIVE);
        room = TestData.room();
        when(currentUser.get()).thenReturn(TestData.admin());
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(member));
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.countActiveFutureBookings(anyLong(), any(), any())).thenReturn(0L);
        when(bookingRepository.findRoomBookingsInRange(anyLong(), any(), any(), any())).thenReturn(List.of());
        when(bookingRepository.sumIncludedHoursApplied(anyLong(), any(), any())).thenReturn(new BigDecimal("19"));
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(TestData.service(1L, "8000")));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(100L);
            return b;
        });
    }

    private BookingRequest request(Long memberId, int startHour, int startMin, int endHour, int endMin) {
        LocalDateTime day = NOW.plusDays(2).withHour(0);
        return new BookingRequest(1L, memberId, day.withHour(startHour).withMinute(startMin),
                day.withHour(endHour).withMinute(endMin), 4, List.of(new BookingServiceRequest(1L, 2)));
    }

    @Test
    @DisplayName("Crea una reserva correctamente y calcula costos (RN-07)")
    void createsBooking() {
        // 2 h; quedan 1 h incluidas (20 - 19) -> 1 h excedente × 50.000 × 0,85 = 42.500
        BookingResponse response = service.create(request(1L, 9, 0, 11, 0));

        assertThat(response.estado()).isEqualTo(BookingStatus.PENDING);
        assertThat(response.horasIncluidasAplicadas()).isEqualByComparingTo("1");
        assertThat(response.costoSala()).isEqualByComparingTo("42500.00");
        assertThat(response.costoServicios()).isEqualByComparingTo("16000.00");
        assertThat(response.costoTotal()).isEqualByComparingTo("58500.00");
        assertThat(response.servicios()).hasSize(1);
        assertThat(response.servicios().getFirst().precioUnitarioAplicado()).isEqualByComparingTo("8000");
    }

    @Test
    @DisplayName("RN-01: evita reservas cruzadas")
    void rejectsOverlap() {
        Booking existing = TestData.booking(5L, member, NOW.plusDays(2).withHour(10), NOW.plusDays(2).withHour(12),
                BookingStatus.CONFIRMED);
        when(bookingRepository.findRoomBookingsInRange(eq(1L), any(), any(), any())).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.create(request(1L, 9, 0, 11, 0)))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("RN-01");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("RN-01: permite reservas consecutivas (10:00-11:00 después de 09:00-10:00)")
    void allowsConsecutive() {
        Booking previous = TestData.booking(5L, member, NOW.plusDays(2).withHour(9), NOW.plusDays(2).withHour(10),
                BookingStatus.PENDING);
        when(bookingRepository.findRoomBookingsInRange(eq(1L), any(), any(), any())).thenReturn(List.of(previous));

        BookingResponse response = service.create(request(1L, 10, 0, 11, 0));
        assertThat(response.id()).isEqualTo(100L);
    }

    @Test
    @DisplayName("RN-02: rechaza una reserva de 45 minutos")
    void rejects45Minutes() {
        assertThatThrownBy(() -> service.create(request(1L, 9, 0, 9, 45)))
                .extracting("code").isEqualTo("RN-02");
    }

    @Test
    @DisplayName("RN-03: rechaza una reserva fuera del horario de la sede")
    void rejectsOutsideOpeningHours() {
        assertThatThrownBy(() -> service.create(request(1L, 5, 0, 6, 0)))
                .extracting("code").isEqualTo("RN-03");
    }

    @Test
    @DisplayName("RN-05: un miembro suspendido no puede reservar")
    void rejectsSuspendedMember() {
        member.setStatus(MemberStatus.SUSPENDED);
        assertThatThrownBy(() -> service.create(request(1L, 9, 0, 10, 0)))
                .extracting("code").isEqualTo("RN-05");
    }

    @Test
    @DisplayName("RN-13: un MEMBER no puede reservar a nombre de otro miembro")
    void memberCannotBookForOthers() {
        when(currentUser.get()).thenReturn(TestData.memberUser(2L));
        assertThatThrownBy(() -> service.create(request(1L, 9, 0, 10, 0)))
                .isInstanceOf(AccessDeniedException.class);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("RN-13: un MEMBER no puede ver reservas de otro miembro")
    void memberCannotSeeOthersBooking() {
        Booking other = TestData.booking(7L, member, NOW.plusDays(1), NOW.plusDays(1).plusHours(1), BookingStatus.PENDING);
        when(bookingRepository.findDetailedById(7L)).thenReturn(Optional.of(other));
        when(currentUser.get()).thenReturn(TestData.memberUser(2L));

        assertThatThrownBy(() -> service.findById(7L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("RN-13: un MEMBER no puede listar con el miembroId de otro")
    void memberCannotListOthers() {
        when(currentUser.get()).thenReturn(TestData.memberUser(2L));
        assertThatThrownBy(() -> service.findAll(1L, null, null, null)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("RN-08: cancelar con 5 h de anticipación cobra el 50 % de la sala")
    void cancelWithPenalty() {
        Booking b = TestData.booking(8L, member, NOW.plusHours(5), NOW.plusHours(7), BookingStatus.CONFIRMED);
        b.setRoomCost(new BigDecimal("68000.00"));
        when(bookingRepository.findDetailedById(8L)).thenReturn(Optional.of(b));

        BookingResponse response = service.cancel(8L);
        assertThat(response.estado()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(response.cargoCancelacion()).isEqualByComparingTo("34000.00");
    }

    @Test
    @DisplayName("RN-08: cancelar con 30 h es gratis y devuelve las horas del plan")
    void cancelFreeRefundsHours() {
        Booking b = TestData.booking(9L, member, NOW.plusHours(30), NOW.plusHours(32), BookingStatus.PENDING);
        b.setIncludedHoursApplied(new BigDecimal("2.00"));
        when(bookingRepository.findDetailedById(9L)).thenReturn(Optional.of(b));

        BookingResponse response = service.cancel(9L);
        assertThat(response.cargoCancelacion()).isEqualByComparingTo("0");
        assertThat(response.horasIncluidasAplicadas()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("RN-09: no se puede confirmar una reserva cancelada")
    void cannotConfirmCancelled() {
        Booking b = TestData.booking(10L, member, NOW.plusDays(3), NOW.plusDays(3).plusHours(1), BookingStatus.CANCELLED);
        when(bookingRepository.findDetailedById(10L)).thenReturn(Optional.of(b));
        assertThatThrownBy(() -> service.confirm(10L)).extracting("code").isEqualTo("RN-09");
    }

    @Test
    @DisplayName("RN-10: no se modifican servicios de una reserva CONFIRMED")
    void cannotEditServicesWhenConfirmed() {
        Booking b = TestData.booking(11L, member, NOW.plusDays(3), NOW.plusDays(3).plusHours(1), BookingStatus.CONFIRMED);
        when(bookingRepository.findDetailedById(11L)).thenReturn(Optional.of(b));
        assertThatThrownBy(() -> service.addService(11L, new BookingServiceRequest(1L, 1)))
                .extracting("code").isEqualTo("RN-10");
    }
}
