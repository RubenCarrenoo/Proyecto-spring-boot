package com.coworkhub.domain;

import com.coworkhub.domain.enums.BookingStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reserva de una sala por parte de un miembro.
 * <p>
 * Además de los atributos del enunciado se persiste {@code includedHoursApplied}: cuántas horas de
 * la reserva se descontaron de las horas incluidas del plan. Hace falta para calcular el consumo
 * mensual y para "devolver las horas" en una cancelación gratuita (RN-08).
 */
@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_booking_room_time", columnList = "room_id, start_time, end_time"),
        @Index(name = "idx_booking_member_time", columnList = "member_id, start_time")
})
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Integer attendees;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "room_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal roomCost = BigDecimal.ZERO;

    @Column(name = "services_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal servicesCost = BigDecimal.ZERO;

    @Column(name = "total_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(name = "cancellation_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal cancellationFee = BigDecimal.ZERO;

    @Column(name = "included_hours_applied", nullable = false, precision = 6, scale = 2)
    private BigDecimal includedHoursApplied = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** Los ítems pertenecen a la reserva: se guardan y eliminan con ella. */
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BookingItem> items = new ArrayList<>();

    public Booking() {
    }

    /** Duración en horas (múltiplos de 0.5). */
    public BigDecimal durationHours() {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    public void addItem(BookingItem item) {
        item.setBooking(this);
        items.add(item);
    }

    public Optional<BookingItem> findItemByServiceId(Long serviceId) {
        return items.stream().filter(i -> i.getService().getId().equals(serviceId)).findFirst();
    }

    public boolean removeItemByServiceId(Long serviceId) {
        return items.removeIf(i -> i.getService().getId().equals(serviceId));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Integer getAttendees() { return attendees; }
    public void setAttendees(Integer attendees) { this.attendees = attendees; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public BigDecimal getRoomCost() { return roomCost; }
    public void setRoomCost(BigDecimal roomCost) { this.roomCost = roomCost; }
    public BigDecimal getServicesCost() { return servicesCost; }
    public void setServicesCost(BigDecimal servicesCost) { this.servicesCost = servicesCost; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public BigDecimal getCancellationFee() { return cancellationFee; }
    public void setCancellationFee(BigDecimal cancellationFee) { this.cancellationFee = cancellationFee; }
    public BigDecimal getIncludedHoursApplied() { return includedHoursApplied; }
    public void setIncludedHoursApplied(BigDecimal includedHoursApplied) { this.includedHoursApplied = includedHoursApplied; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public List<BookingItem> getItems() { return items; }
}
