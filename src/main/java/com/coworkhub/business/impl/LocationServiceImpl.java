package com.coworkhub.business.impl;

import com.coworkhub.business.LocationService;
import com.coworkhub.domain.Location;
import com.coworkhub.dto.catalog.LocationRequest;
import com.coworkhub.dto.catalog.LocationResponse;
import com.coworkhub.exception.ResourceInUseException;
import com.coworkhub.exception.ResourceNotFoundException;
import com.coworkhub.mapper.CatalogMapper;
import com.coworkhub.persistence.LocationRepository;
import com.coworkhub.persistence.RoomRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final RoomRepository roomRepository;
    private final CatalogMapper mapper;

    public LocationServiceImpl(LocationRepository locationRepository, RoomRepository roomRepository,
                               CatalogMapper mapper) {
        this.locationRepository = locationRepository;
        this.roomRepository = roomRepository;
        this.mapper = mapper;
    }

    @Override
    public List<LocationResponse> findAll() {
        return locationRepository.findAll(Sort.by("id")).stream().map(mapper::toResponse).toList();
    }

    @Override
    public LocationResponse findById(Long id) {
        return mapper.toResponse(get(id));
    }

    @Override
    @Transactional
    public LocationResponse create(LocationRequest request) {
        Location location = new Location();
        mapper.apply(request, location);
        return mapper.toResponse(locationRepository.save(location));
    }

    @Override
    @Transactional
    public LocationResponse update(Long id, LocationRequest request) {
        Location location = get(id);
        mapper.apply(request, location);
        return mapper.toResponse(location);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Location location = get(id);
        if (roomRepository.existsByLocationId(id)) {
            throw new ResourceInUseException("No se puede eliminar la sede porque tiene salas registradas");
        }
        locationRepository.delete(location);
    }

    private Location get(Long id) {
        return locationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sede", id));
    }
}
