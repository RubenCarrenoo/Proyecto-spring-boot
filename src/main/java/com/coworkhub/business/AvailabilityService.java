package com.coworkhub.business;

import com.coworkhub.dto.catalog.RoomResponse;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AvailabilityService {

    /**
     * Salas activas de la sede, libres en el rango indicado, con capacidad mínima y con TODO el
     * equipamiento solicitado.
     */
    @PreAuthorize("isAuthenticated()")
    List<RoomResponse> findAvailableRooms(Long locationId, LocalDate date, LocalTime start, LocalTime end,
                                          Integer minCapacity, List<Long> equipmentIds);
}
