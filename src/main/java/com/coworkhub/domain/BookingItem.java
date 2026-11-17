package com.coworkhub.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Línea de servicio adicional dentro de una reserva.
 * appliedUnitPrice guarda el precio vigente al agregarlo (precio histórico, RN-07).
 */
@Entity
@Table(name = "booking_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_booking_item_service", columnNames = {"booking_id", "service_id"}))
public class BookingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "applied_unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal appliedUnitPrice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private AdditionalService service;

    public BookingItem() {
    }

    public BookingItem(AdditionalService service, int quantity) {
        this.service = service;
        this.quantity = quantity;
        this.appliedUnitPrice = service.getUnitPrice();
    }

    public BigDecimal subtotal() {
        return appliedUnitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getAppliedUnitPrice() { return appliedUnitPrice; }
    public void setAppliedUnitPrice(BigDecimal appliedUnitPrice) { this.appliedUnitPrice = appliedUnitPrice; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public AdditionalService getService() { return service; }
    public void setService(AdditionalService service) { this.service = service; }
}
