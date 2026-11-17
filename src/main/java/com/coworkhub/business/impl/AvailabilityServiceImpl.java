package com.coworkhub.business.impl;

import com.coworkhub.business.AvailabilityService;
import com.coworkhub.business.rules.BookingRules;
import com.coworkhub.domain.Equipment;
import com.coworkhub.domain.Location;
import com.coworkhub.domain.enums.BookingStatus;
import com.coworkhub.dto.catalog.RoomResponse;
import com.coworkhub.exception.ResourceNotFoundException;
import com.coworkhub.mapper.CatalogMapper;
import com.coworkhub.persistence.BookingRepository;
import com.coworkhub.persistence.LocationRepository;
import com.coworkhub.persistence.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AvailabilityServiceImpl implements AvailabilityService {

    private final LocationRepository locationRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final BookingRules rules;
    private final CatalogMapper mapper;

    public AvailabilityServiceImpl(LocationRepository locationRepository, RoomRepository roomRepository,
                                   BookingRepository bookingRepository, BookingRules rules, CatalogMapper mapper) {
        this.locationRepository = locationRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.rules = rules;
        this.mapper = mapper;
    }

    @Override
    public List<RoomResponse> findAvailableRooms(Long locationId, LocalDate date, LocalTime start, LocalTime end,
                                                 Integer minCapacity, List<Long> equipmentIds) {
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("horaInicio debe ser anterior a horaFin");
        }
        if (minCapacity != null && minCapacity < 1) {
            throw new IllegalArgumentException("capacidadMinima debe ser mayor que 0");
        }
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Sede", locationId));
        rules.validateWithinOpeningHours(location, start, end);

        LocalDateTime from = date.atTime(start);
        LocalDateTime to = date.atTime(end);
        Set<Long> busy = new HashSet<>(bookingRepository.findBusyRoomIds(locationId, from, to, BookingStatus.ACTIVE));
        Set<Long> required = equipmentIds == null ? Set.of() : new HashSet<>(equipmentIds);
        int capacity = minCapacity == null ? 1 : minCapacity;

        return roomRepository.findActiveByLocationAndMinCapacity(locationId, capacity).stream()
                .filter(r -> !busy.contains(r.getId()))
                .filter(r -> r.getEquipment().stream().map(Equipment::getId).collect(Collectors.toSet()).containsAll(required))
                .map(mapper::toResponse)
                .toList();
    }
}
