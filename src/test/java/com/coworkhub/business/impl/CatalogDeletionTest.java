package com.coworkhub.business.impl;

import com.coworkhub.TestData;
import com.coworkhub.domain.Equipment;
import com.coworkhub.domain.enums.MemberStatus;
import com.coworkhub.exception.ResourceInUseException;
import com.coworkhub.mapper.CatalogMapper;
import com.coworkhub.mapper.MemberMapper;
import com.coworkhub.persistence.*;
import com.coworkhub.security.CurrentUserProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** RN-12: no se eliminan registros en uso. */
@ExtendWith(MockitoExtension.class)
class CatalogDeletionTest {

    @Mock RoomRepository roomRepository;
    @Mock LocationRepository locationRepository;
    @Mock EquipmentRepository equipmentRepository;
    @Mock BookingRepository bookingRepository;
    @Mock BookingItemRepository bookingItemRepository;
    @Mock AdditionalServiceRepository serviceRepository;
    @Mock MembershipPlanRepository planRepository;
    @Mock MemberRepository memberRepository;
    @Mock UserAccountRepository accountRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock CurrentUserProvider currentUser;

    private final CatalogMapper mapper = new CatalogMapper();

    @Test
    @DisplayName("No se elimina una sala con reservas")
    void roomWithBookings() {
        var service = new RoomServiceImpl(roomRepository, locationRepository, equipmentRepository, bookingRepository, mapper);
        when(roomRepository.findWithDetailsById(1L)).thenReturn(Optional.of(TestData.room()));
        when(bookingRepository.existsByRoomId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ResourceInUseException.class)
                .extracting("code").isEqualTo("RN-12");
        verify(roomRepository, never()).delete(any());
    }

    @Test
    @DisplayName("No se elimina una sede con salas")
    void locationWithRooms() {
        var service = new LocationServiceImpl(locationRepository, roomRepository, mapper);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(TestData.location()));
        when(roomRepository.existsByLocationId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ResourceInUseException.class);
        verify(locationRepository, never()).delete(any());
    }

    @Test
    @DisplayName("No se elimina un equipamiento asignado a salas")
    void equipmentInUse() {
        var service = new EquipmentServiceImpl(equipmentRepository, roomRepository, mapper);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(new Equipment(1L, "Proyector")));
        when(roomRepository.existsByEquipmentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ResourceInUseException.class);
    }

    @Test
    @DisplayName("No se elimina un servicio incluido en reservas")
    void serviceInUse() {
        var service = new AdditionalServiceCatalogServiceImpl(serviceRepository, bookingItemRepository, mapper);
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(TestData.service(1L, "8000")));
        when(bookingItemRepository.existsByServiceId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ResourceInUseException.class);
    }

    @Test
    @DisplayName("No se elimina un plan con miembros")
    void planInUse() {
        var service = new MembershipPlanServiceImpl(planRepository, memberRepository, mapper);
        when(planRepository.findById(2L)).thenReturn(Optional.of(TestData.plan(20, "0.15", 5)));
        when(memberRepository.existsByPlanId(2L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(2L)).isInstanceOf(ResourceInUseException.class);
    }

    @Test
    @DisplayName("No se elimina un miembro con reservas")
    void memberWithBookings() {
        var service = new MemberServiceImpl(memberRepository, planRepository, accountRepository, bookingRepository,
                passwordEncoder, currentUser, new MemberMapper());
        when(memberRepository.findWithPlanById(1L)).thenReturn(Optional.of(TestData.member(1L, MemberStatus.ACTIVE)));
        when(bookingRepository.existsByMemberId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ResourceInUseException.class);
        verify(memberRepository, never()).delete(any());
    }
}
