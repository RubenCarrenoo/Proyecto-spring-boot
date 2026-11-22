package com.coworkhub.business;

import com.coworkhub.dto.catalog.RoomRequest;
import com.coworkhub.dto.catalog.RoomResponse;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@PreAuthorize("isAuthenticated()")
public interface RoomService {

    /** @param locationId filtro opcional por sede */
    List<RoomResponse> findAll(Long locationId);

    RoomResponse findById(Long id);

    @PreAuthorize("hasRole('ADMIN')")
    RoomResponse create(RoomRequest request);

    @PreAuthorize("hasRole('ADMIN')")
    RoomResponse update(Long id, RoomRequest request);

    @PreAuthorize("hasRole('ADMIN')")
    void delete(Long id);
}
