package com.coworkhub.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Plan de membresía.
 * <ul>
 *   <li>includedHoursPerMonth: horas de sala sin costo cada mes.</li>
 *   <li>overageDiscount: descuento (entre 0 y 1) sobre la tarifa por hora de las horas excedentes.</li>
 *   <li>maxActiveBookings: máximo de reservas activas futuras (RN-06).</li>
 * </ul>
 */
@Entity
@Table(name = "membership_plans")
public class MembershipPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(name = "included_hours_per_month", nullable = false)
    private Integer includedHoursPerMonth;

    @Column(name = "overage_discount", nullable = false, precision = 5, scale = 4)
    private BigDecimal overageDiscount;

    @Column(name = "max_active_bookings", nullable = false)
    private Integer maxActiveBookings;

    @OneToMany(mappedBy = "plan", fetch = FetchType.LAZY)
    private List<Member> members = new ArrayList<>();

    public MembershipPlan() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getIncludedHoursPerMonth() { return includedHoursPerMonth; }
    public void setIncludedHoursPerMonth(Integer includedHoursPerMonth) { this.includedHoursPerMonth = includedHoursPerMonth; }
    public BigDecimal getOverageDiscount() { return overageDiscount; }
    public void setOverageDiscount(BigDecimal overageDiscount) { this.overageDiscount = overageDiscount; }
    public Integer getMaxActiveBookings() { return maxActiveBookings; }
    public void setMaxActiveBookings(Integer maxActiveBookings) { this.maxActiveBookings = maxActiveBookings; }
    public List<Member> getMembers() { return members; }
}
