package com.coworkhub.persistence;

import com.coworkhub.domain.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    /** Carga sede y equipamiento en la misma consulta (evita N+1 al mapear a DTO). */
    @EntityGraph(attributePaths = {"location", "equipment"})
    @Query("select distinct r from Room r order by r.id")
    List<Room> findAllWithDetails();

    @EntityGraph(attributePaths = {"location", "equipment"})
    @Query("select distinct r from Room r where r.location.id = :locationId order by r.id")
    List<Room> findAllWithDetailsByLocationId(@Param("locationId") Long locationId);

    @EntityGraph(attributePaths = {"location", "equipment"})
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findWithDetailsById(@Param("id") Long id);

    /**
     * Bloqueo pesimista (SELECT ... FOR UPDATE) sobre la fila de la sala.
     * Serializa la creación de reservas para una misma sala y evita la condición de carrera
     * "leer disponibilidad -> insertar" entre transacciones simultáneas (RN-01).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r join fetch r.location where r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);

    /** Salas candidatas para la consulta de disponibilidad. */
    @EntityGraph(attributePaths = {"location", "equipment"})
    @Query("""
            select distinct r from Room r
            where r.location.id = :locationId
              and r.active = true
              and r.capacity >= :minCapacity
            order by r.id""")
    List<Room> findActiveByLocationAndMinCapacity(@Param("locationId") Long locationId,
                                                  @Param("minCapacity") int minCapacity);

    boolean existsByLocationIdAndNameIgnoreCase(Long locationId, String name);

    boolean existsByLocationIdAndNameIgnoreCaseAndIdNot(Long locationId, String name, Long id);

    boolean existsByLocationId(Long locationId);

    @Query("select count(r) > 0 from Room r join r.equipment e where e.id = :equipmentId")
    boolean existsByEquipmentId(@Param("equipmentId") Long equipmentId);
}
