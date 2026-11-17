package com.coworkhub.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Servicio adicional que se puede agregar a una reserva (café, impresiones...). */
@Entity
@Table(name = "additional_services")
public class AdditionalService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @OneToMany(mappedBy = "service", fetch = FetchType.LAZY)
    private List<BookingItem> bookingItems = new ArrayList<>();

    public AdditionalService() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public List<BookingItem> getBookingItems() { return bookingItems; }
}
