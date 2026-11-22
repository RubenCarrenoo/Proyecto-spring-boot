package com.coworkhub.mapper;

import com.coworkhub.domain.AdditionalService;
import com.coworkhub.domain.Equipment;
import com.coworkhub.domain.Location;
import com.coworkhub.domain.MembershipPlan;
import com.coworkhub.domain.Room;
import com.coworkhub.dto.catalog.AdditionalServiceRequest;
import com.coworkhub.dto.catalog.AdditionalServiceResponse;
import com.coworkhub.dto.catalog.EquipmentResponse;
import com.coworkhub.dto.catalog.LocationRequest;
import com.coworkhub.dto.catalog.LocationResponse;
import com.coworkhub.dto.catalog.MembershipPlanRequest;
import com.coworkhub.dto.catalog.MembershipPlanResponse;
import com.coworkhub.dto.catalog.RoomResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/** Conversión entre entidades de catálogo y sus DTO. Nunca se exponen entidades JPA. */
@Component
public class CatalogMapper {

    public LocationResponse toResponse(Location l) {
        return new LocationResponse(l.getId(), l.getName(), l.getCity(), l.getAddress(),
                l.getOpeningTime(), l.getClosingTime());
    }

    public void apply(LocationRequest r, Location l) {
        l.setName(r.nombre().trim());
        l.setCity(r.ciudad().trim());
        l.setAddress(r.direccion().trim());
        l.setOpeningTime(r.horaApertura());
        l.setClosingTime(r.horaCierre());
    }

    public RoomResponse toResponse(Room r) {
        List<EquipmentResponse> equipment = r.getEquipment().stream()
                .sorted(Comparator.comparing(Equipment::getId))
                .map(this::toResponse)
                .toList();
        return new RoomResponse(r.getId(), r.getName(), r.getType(), r.getCapacity(), r.getHourlyRate(),
                r.isActive(), r.getLocation().getId(), r.getLocation().getName(), equipment);
    }

    public EquipmentResponse toResponse(Equipment e) {
        return new EquipmentResponse(e.getId(), e.getName());
    }

    public AdditionalServiceResponse toResponse(AdditionalService s) {
        return new AdditionalServiceResponse(s.getId(), s.getName(), s.getUnitPrice());
    }

    public void apply(AdditionalServiceRequest r, AdditionalService s) {
        s.setName(r.nombre().trim());
        s.setUnitPrice(r.precioUnitario());
    }

    public MembershipPlanResponse toResponse(MembershipPlan p) {
        return new MembershipPlanResponse(p.getId(), p.getName(), p.getIncludedHoursPerMonth(),
                p.getOverageDiscount(), p.getMaxActiveBookings());
    }

    public void apply(MembershipPlanRequest r, MembershipPlan p) {
        p.setName(r.nombre().trim());
        p.setIncludedHoursPerMonth(r.horasIncluidasMes());
        p.setOverageDiscount(r.descuentoExcedente());
        p.setMaxActiveBookings(r.maxReservasActivas());
    }
}
