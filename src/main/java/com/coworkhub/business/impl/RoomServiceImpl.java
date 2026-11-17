package com.coworkhub.business.impl;

import com.coworkhub.business.RoomService;
import com.coworkhub.domain.Equipment;
import com.coworkhub.domain.Location;
import com.coworkhub.domain.Room;
import com.coworkhub.dto.catalog.RoomRequest;
import com.coworkhub.dto.catalog.RoomResponse;
import com.coworkhub.exception.DuplicateResourceException;
import com.coworkhub.exception.ResourceInUseException;
import com.coworkhub.exception.ResourceNotFoundException;
import com.coworkhub.mapper.CatalogMapper;
import com.coworkhub.persistence.BookingRepository;
import com.coworkhub.persistence.EquipmentRepository;
import com.coworkhub.persistence.LocationRepository;
import com.coworkhub.persistence.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final LocationRepository locationRepository;
    private final EquipmentRepository equipmentRepository;
    private final BookingRepository bookingRepository;
    private final CatalogMapper mapper;

    public RoomServiceImpl(RoomRepository roomRepository, LocationRepository locationRepository,
                           EquipmentRepository equipmentRepository, BookingRepository bookingRepository,
                           CatalogMapper mapper) {
        this.roomRepository = roomRepository;
        this.locationRepository = locationRepository;
        this.equipmentRepository = equipmentRepository;
        this.bookingRepository = bookingRepository;
        this.mapper = mapper;
    }

    @Override
    public List<RoomResponse> findAll(Long locationId) {
        List<Room> rooms = locationId == null
                ? roomRepository.findAllWithDetails()
                : roomRepository.findAllWithDetailsByLocationId(locationId);
        return rooms.stream().map(mapper::toResponse).toList();
    }

    @Override
    public RoomResponse findById(Long id) {
        return mapper.toResponse(getDetailed(id));
    }

    @Override
    @Transactional
    public RoomResponse create(RoomRequest request) {
        Location location = getLocation(request.sedeId());
        String name = request.nombre().trim();
        if (roomRepository.existsByLocationIdAndNameIgnoreCase(location.getId(), name)) {
            throw new DuplicateResourceException("Ya existe una sala llamada '" + name + "' en la sede " + location.getName());
        }
        Room room = new Room();
        apply(request, room, location);
        return mapper.toResponse(roomRepository.save(room));
    }

    @Override
    @Transactional
    public RoomResponse update(Long id, RoomRequest request) {
        Room room = getDetailed(id);
        Location location = getLocation(request.sedeId());
        String name = request.nombre().trim();
        if (roomRepository.existsByLocationIdAndNameIgnoreCaseAndIdNot(location.getId(), name, id)) {
            throw new DuplicateResourceException("Ya existe una sala llamada '" + name + "' en la sede " + location.getName());
        }
        apply(request, room, location);
        return mapper.toResponse(room);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Room room = getDetailed(id);
        if (bookingRepository.existsByRoomId(id)) {
            throw new ResourceInUseException("No se puede eliminar la sala porque tiene reservas registradas");
        }
        roomRepository.delete(room);
    }

    private void apply(RoomRequest request, Room room, Location location) {
        room.setName(request.nombre().trim());
        room.setType(request.tipo());
        room.setCapacity(request.capacidad());
        room.setHourlyRate(request.tarifaHora());
        room.setActive(request.activa() == null || request.activa());
        room.setLocation(location);
        room.getEquipment().clear();
        room.getEquipment().addAll(resolveEquipment(request.equipamientoIds()));
    }

    private Set<Equipment> resolveEquipment(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        Set<Long> unique = new LinkedHashSet<>(ids);
        List<Equipment> found = equipmentRepository.findAllById(unique);
        if (found.size() != unique.size()) {
            Set<Long> missing = new HashSet<>(unique);
            found.forEach(e -> missing.remove(e.getId()));
            throw new ResourceNotFoundException("Equipamiento", missing);
        }
        return new LinkedHashSet<>(found);
    }

    private Room getDetailed(Long id) {
        return roomRepository.findWithDetailsById(id).orElseThrow(() -> new ResourceNotFoundException("Sala", id));
    }

    private Location getLocation(Long id) {
        return locationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sede", id));
    }
}
