package com.coworkhub.mapper;

import com.coworkhub.domain.Booking;
import com.coworkhub.domain.BookingItem;
import com.coworkhub.dto.booking.BookingItemResponse;
import com.coworkhub.dto.booking.BookingResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking b) {
        List<BookingItemResponse> items = b.getItems().stream()
                .sorted(Comparator.comparing(i -> i.getService().getId()))
                .map(this::toResponse)
                .toList();
        return new BookingResponse(
                b.getId(),
                b.getRoom().getId(),
                b.getRoom().getName(),
                b.getRoom().getLocation().getId(),
                b.getRoom().getLocation().getName(),
                b.getMember().getId(),
                b.getMember().getFullName(),
                b.getStartTime(),
                b.getEndTime(),
                b.durationHours(),
                b.getAttendees(),
                b.getStatus(),
                b.getIncludedHoursApplied(),
                b.getRoomCost(),
                b.getServicesCost(),
                b.getTotalCost(),
                b.getCancellationFee(),
                items);
    }

    public BookingItemResponse toResponse(BookingItem i) {
        return new BookingItemResponse(i.getService().getId(), i.getService().getName(), i.getQuantity(),
                i.getAppliedUnitPrice(), i.subtotal());
    }
}
