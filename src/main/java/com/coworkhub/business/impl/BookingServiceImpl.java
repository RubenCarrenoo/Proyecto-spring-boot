package com.coworkhub.business.impl;

import com.coworkhub.business.BookingService;
import com.coworkhub.business.rules.BookingRules;
import com.coworkhub.business.rules.CancellationPolicy;
import com.coworkhub.business.rules.CostCalculator;
import com.coworkhub.domain.AdditionalService;
import com.coworkhub.domain.Booking;
import com.coworkhub.domain.BookingItem;
import com.coworkhub.domain.Member;
import com.coworkhub.domain.Room;
import com.coworkhub.domain.enums.BookingStatus;
import com.coworkhub.dto.booking.BookingRequest;
import com.coworkhub.dto.booking.BookingResponse;
import com.coworkhub.dto.booking.BookingServiceRequest;
import com.coworkhub.exception.ResourceNotFoundException;
import com.coworkhub.mapper.BookingMapper;
import com.coworkhub.persistence.AdditionalServiceRepository;
import com.coworkhub.persistence.BookingRepository;
import com.coworkhub.persistence.MemberRepository;
import com.coworkhub.persistence.RoomRepository;
import com.coworkhub.security.AuthenticatedUser;
import com.coworkhub.security.CurrentUserProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private static final LocalDateTime MIN_DATE = LocalDateTime.of(1900, 1, 1, 0, 0);
    private static final LocalDateTime MAX_DATE = LocalDateTime.of(9999, 12, 31, 0, 0);

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final MemberRepository memberRepository;
    private final AdditionalServiceRepository serviceRepository;
    private final BookingRules rules;
    private final CostCalculator costCalculator;
    private final CancellationPolicy cancellationPolicy;
    private final CurrentUserProvider currentUser;
    private final BookingMapper mapper;
    private final Clock clock;

    public BookingServiceImpl(BookingRepository bookingRepository, RoomRepository roomRepository,
                              MemberRepository memberRepository, AdditionalServiceRepository serviceRepository,
                              BookingRules rules, CostCalculator costCalculator,
                              CancellationPolicy cancellationPolicy, CurrentUserProvider currentUser,
                              BookingMapper mapper, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
        this.serviceRepository = serviceRepository;
        this.rules = rules;
        this.costCalculator = costCalculator;
        this.cancellationPolicy = cancellationPolicy;
        this.currentUser = currentUser;
        this.mapper = mapper;
        this.clock = clock;
    }

    /**
     * Crea una reserva aplicando RN-01 a RN-07.
     * <p>
     * Concurrencia (RN-01 y RN-06): se bloquean con SELECT ... FOR UPDATE la fila del miembro y la
     * de la sala, siempre en ese orden. Dos solicitudes simultáneas para la misma sala quedan
     * serializadas: la segunda espera a que la primera confirme y, gracias al aislamiento
     * READ_COMMITTED, ve la reserva recién creada y recibe 409.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse create(BookingRequest request) {
        Long memberId = resolveMemberId(request.miembroId());
        LocalDateTime start = request.inicio();
        LocalDateTime end = request.fin();
        LocalDateTime now = LocalDateTime.now(clock);

        rules.validateDuration(start, end);                       // RN-02
        rules.validateAdvance(start, now);                        // RN-03 (futura, 1 h de anticipación)

        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Miembro", memberId));
        rules.validateMemberActive(member);                       // RN-05

        Room room = roomRepository.findByIdForUpdate(request.salaId())
                .orElseThrow(() -> new ResourceNotFoundException("Sala", request.salaId()));
        rules.validateRoom(room, request.asistentes());           // RN-04
        rules.validateWithinOpeningHours(room.getLocation(), start.toLocalTime(), end.toLocalTime()); // RN-03

        long active = bookingRepository.countActiveFutureBookings(memberId, now, BookingStatus.ACTIVE);
        rules.validateActiveBookingLimit(active, member.getPlan()); // RN-06

        List<Booking> sameRange = bookingRepository.findRoomBookingsInRange(room.getId(), start, end, BookingStatus.ACTIVE);
        rules.validateNoOverlap(sameRange, start, end);           // RN-01

        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setMember(member);
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setAttendees(request.asistentes());
        booking.setStatus(BookingStatus.PENDING);
        if (request.servicios() != null) {
            mergeServiceRequests(request.servicios()).forEach((serviceId, qty) ->
                    booking.addItem(new BookingItem(getService(serviceId), qty)));
        }
        applyRoomCost(booking, member);                           // RN-07
        recalculateTotals(booking);

        Booking saved = bookingRepository.save(booking);
        return mapper.toResponse(saved);
    }

    @Override
    public List<BookingResponse> findAll(Long memberId, Long roomId, LocalDate date, BookingStatus status) {
        AuthenticatedUser user = currentUser.get();
        Long effectiveMember = memberId;
        if (user.isMember()) {
            if (memberId != null && !memberId.equals(user.getMemberId())) {
                throw new AccessDeniedException("No puede consultar reservas de otro miembro");
            }
            effectiveMember = user.getMemberId();
        }
        LocalDateTime from = date == null ? MIN_DATE : date.atStartOfDay();
        LocalDateTime to = date == null ? MAX_DATE : date.plusDays(1).atStartOfDay();
        return bookingRepository.search(effectiveMember, roomId, status, from, to).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public BookingResponse findById(Long id) {
        return mapper.toResponse(getOwned(id));
    }

    @Override
    @Transactional
    public BookingResponse addService(Long bookingId, BookingServiceRequest request) {
        Booking booking = getOwned(bookingId);
        rules.validateServicesEditable(booking);                  // RN-10
        AdditionalService service = getService(request.servicioId());
        booking.findItemByServiceId(service.getId()).ifPresentOrElse(
                item -> item.setQuantity(item.getQuantity() + request.cantidad()),
                () -> booking.addItem(new BookingItem(service, request.cantidad())));
        recalculateTotals(booking);
        return mapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse removeService(Long bookingId, Long serviceId) {
        Booking booking = getOwned(bookingId);
        rules.validateServicesEditable(booking);                  // RN-10
        if (!booking.removeItemByServiceId(serviceId)) {
            throw new ResourceNotFoundException("Servicio adicional en la reserva " + bookingId + "; servicio", serviceId);
        }
        recalculateTotals(booking);
        return mapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse confirm(Long id) {
        Booking booking = getDetailed(id);
        rules.validateTransition(booking.getStatus(), BookingStatus.CONFIRMED); // RN-09
        booking.setStatus(BookingStatus.CONFIRMED);
        return mapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse cancel(Long id) {
        Booking booking = getOwned(id);
        rules.validateTransition(booking.getStatus(), BookingStatus.CANCELLED); // RN-09
        CancellationPolicy.Result result = cancellationPolicy.evaluate(
                booking.getStartTime(), booking.getRoomCost(), LocalDateTime.now(clock)); // RN-08
        booking.setCancellationFee(result.fee());
        if (result.refundIncludedHours()) {
            booking.setIncludedHoursApplied(BigDecimal.ZERO.setScale(2));
        }
        booking.setStatus(BookingStatus.CANCELLED);
        return mapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse complete(Long id) {
        Booking booking = getDetailed(id);
        rules.validateTransition(booking.getStatus(), BookingStatus.COMPLETED); // RN-09
        booking.setStatus(BookingStatus.COMPLETED);
        return mapper.toResponse(booking);
    }

    // ------------------------------------------------------------------ helpers

    /** Un MEMBER solo reserva a su nombre; el personal debe indicar el miembro. */
    private Long resolveMemberId(Long requested) {
        AuthenticatedUser user = currentUser.get();
        if (user.isMember()) {
            if (requested != null && !requested.equals(user.getMemberId())) {
                throw new AccessDeniedException("Un miembro solo puede crear reservas a su nombre");
            }
            if (user.getMemberId() == null) {
                throw new AccessDeniedException("La cuenta no está vinculada a un miembro");
            }
            return user.getMemberId();
        }
        if (requested == null) {
            throw new IllegalArgumentException("miembroId: es obligatorio cuando reserva el personal");
        }
        return requested;
    }

    /** RN-07: horas incluidas disponibles del mes y costo de la sala. */
    private void applyRoomCost(Booking booking, Member member) {
        YearMonth month = YearMonth.from(booking.getStartTime());
        BigDecimal used = bookingRepository.sumIncludedHoursApplied(member.getId(),
                month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay());
        BigDecimal available = BigDecimal.valueOf(member.getPlan().getIncludedHoursPerMonth()).subtract(used);
        CostCalculator.RoomCost cost = costCalculator.calculateRoomCost(booking.durationHours(), available,
                booking.getRoom().getHourlyRate(), member.getPlan().getOverageDiscount());
        booking.setIncludedHoursApplied(cost.includedHours());
        booking.setRoomCost(cost.roomCost());
    }

    private void recalculateTotals(Booking booking) {
        booking.setServicesCost(costCalculator.calculateServicesCost(booking.getItems()));
        booking.setTotalCost(booking.getRoomCost().add(booking.getServicesCost()));
    }

    /** Agrupa servicios repetidos en la solicitud sumando sus cantidades. */
    private Map<Long, Integer> mergeServiceRequests(List<BookingServiceRequest> requests) {
        Map<Long, Integer> merged = new LinkedHashMap<>();
        requests.forEach(r -> merged.merge(r.servicioId(), r.cantidad(), Integer::sum));
        return merged;
    }

    private Booking getDetailed(Long id) {
        return bookingRepository.findDetailedById(id).orElseThrow(() -> new ResourceNotFoundException("Reserva", id));
    }

    /** Carga la reserva y verifica que un MEMBER solo acceda a las suyas (RN-13). */
    private Booking getOwned(Long id) {
        Booking booking = getDetailed(id);
        AuthenticatedUser user = currentUser.get();
        if (user.isMember() && !booking.getMember().getId().equals(user.getMemberId())) {
            throw new AccessDeniedException("No puede acceder a reservas de otro miembro");
        }
        return booking;
    }

    private AdditionalService getService(Long id) {
        return serviceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Servicio adicional", id));
    }
}
