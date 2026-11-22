package com.coworkhub.business;

import com.coworkhub.domain.enums.BookingStatus;
import com.coworkhub.dto.booking.BookingRequest;
import com.coworkhub.dto.booking.BookingResponse;
import com.coworkhub.dto.booking.BookingServiceRequest;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.util.List;

/**
 * Gestión de reservas. Todos los métodos requieren autenticación; confirmar y completar
 * son exclusivos del personal. La propiedad de la reserva (MEMBER) se valida en la implementación.
 */
@PreAuthorize("isAuthenticated()")
public interface BookingService {

    BookingResponse create(BookingRequest request);

    List<BookingResponse> findAll(Long memberId, Long roomId, LocalDate date, BookingStatus status);

    BookingResponse findById(Long id);

    BookingResponse addService(Long bookingId, BookingServiceRequest request);

    BookingResponse removeService(Long bookingId, Long serviceId);

    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    BookingResponse confirm(Long id);

    BookingResponse cancel(Long id);

    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    BookingResponse complete(Long id);
}
