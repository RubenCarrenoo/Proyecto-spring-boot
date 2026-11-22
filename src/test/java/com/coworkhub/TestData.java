package com.coworkhub;

import com.coworkhub.domain.AdditionalService;
import com.coworkhub.domain.Booking;
import com.coworkhub.domain.Location;
import com.coworkhub.domain.Member;
import com.coworkhub.domain.MembershipPlan;
import com.coworkhub.domain.Room;
import com.coworkhub.domain.enums.BookingStatus;
import com.coworkhub.domain.enums.MemberStatus;
import com.coworkhub.domain.enums.Role;
import com.coworkhub.domain.enums.RoomType;
import com.coworkhub.security.AuthenticatedUser;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Fábrica de objetos de prueba. */
public final class TestData {

    private TestData() {
    }

    public static Location location() {
        Location l = new Location();
        l.setId(1L);
        l.setName("CoWork Centro");
        l.setCity("Bucaramanga");
        l.setAddress("Calle 35");
        l.setOpeningTime(LocalTime.of(7, 0));
        l.setClosingTime(LocalTime.of(20, 0));
        return l;
    }

    public static Room room() {
        Room r = new Room();
        r.setId(1L);
        r.setName("Sala Andes");
        r.setType(RoomType.MEETING_ROOM);
        r.setCapacity(8);
        r.setHourlyRate(new BigDecimal("50000.00"));
        r.setActive(true);
        r.setLocation(location());
        return r;
    }

    public static MembershipPlan plan(int includedHours, String discount, int maxActive) {
        MembershipPlan p = new MembershipPlan();
        p.setId(2L);
        p.setName("Profesional");
        p.setIncludedHoursPerMonth(includedHours);
        p.setOverageDiscount(new BigDecimal(discount));
        p.setMaxActiveBookings(maxActive);
        return p;
    }

    public static Member member(Long id, MemberStatus status) {
        Member m = new Member();
        m.setId(id);
        m.setDocumentNumber("10980000" + id);
        m.setFullName("Miembro " + id);
        m.setEmail("m" + id + "@mail.com");
        m.setStatus(status);
        m.setPlan(plan(20, "0.15", 5));
        return m;
    }

    public static AdditionalService service(Long id, String price) {
        AdditionalService s = new AdditionalService();
        s.setId(id);
        s.setName("Servicio " + id);
        s.setUnitPrice(new BigDecimal(price));
        return s;
    }

    public static Booking booking(Long id, Member member, LocalDateTime start, LocalDateTime end, BookingStatus status) {
        Booking b = new Booking();
        b.setId(id);
        b.setMember(member);
        b.setRoom(room());
        b.setStartTime(start);
        b.setEndTime(end);
        b.setAttendees(2);
        b.setStatus(status);
        return b;
    }

    public static AuthenticatedUser admin() {
        return new AuthenticatedUser(1L, "admin", "x", Role.ADMIN, null);
    }

    public static AuthenticatedUser receptionist() {
        return new AuthenticatedUser(2L, "recepcion", "x", Role.RECEPTIONIST, null);
    }

    public static AuthenticatedUser memberUser(Long memberId) {
        return new AuthenticatedUser(10L + memberId, "member" + memberId, "x", Role.MEMBER, memberId);
    }
}
