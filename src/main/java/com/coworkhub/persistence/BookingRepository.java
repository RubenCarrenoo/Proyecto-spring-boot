package com.coworkhub.persistence;

import com.coworkhub.domain.Booking;
import com.coworkhub.domain.enums.BookingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /*
     * Los @EntityGraph cargan sala, sede, miembro e ítems en una sola consulta (evita N+1).
     * "member.userAccount" es necesario: Member.userAccount es el lado NO dueño de un 1:1 y Hibernate
     * no puede cargarlo de forma perezosa (no sabe si existe sin consultar). Sin incluirlo en el grafo
     * se emitía un SELECT a user_accounts por cada reserva listada.
     */

    @EntityGraph(attributePaths = {"room", "room.location", "member", "member.userAccount", "items", "items.service"})
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"room", "room.location", "member", "member.userAccount", "items", "items.service"})
    @Query("select distinct b from Booking b order by b.startTime desc")
    List<Booking> findAllDetailed();

    @EntityGraph(attributePaths = {"room", "room.location", "member", "member.userAccount", "items", "items.service"})
    @Query("""
            select distinct b from Booking b
            where (:memberId is null or b.member.id = :memberId)
              and (:roomId is null or b.room.id = :roomId)
              and (:status is null or b.status = :status)
              and b.startTime >= :from
              and b.startTime < :to
            order by b.startTime desc""")
    List<Booking> search(@Param("memberId") Long memberId,
                         @Param("roomId") Long roomId,
                         @Param("status") BookingStatus status,
                         @Param("from") LocalDateTime from,
                         @Param("to") LocalDateTime to);

    /**
     * Reservas de la sala que ocupan el rango [from, to) en los estados indicados.
     * El cruce se evalúa con desigualdades estrictas: una reserva que termina justo cuando
     * empieza otra NO se cruza (RN-01).
     */
    @Query("""
            select b from Booking b
            where b.room.id = :roomId
              and b.status in :statuses
              and b.startTime < :to
              and b.endTime > :from""")
    List<Booking> findRoomBookingsInRange(@Param("roomId") Long roomId,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to,
                                          @Param("statuses") Collection<BookingStatus> statuses);

    /** Ids de salas de una sede ocupadas en [from, to). Usado por la consulta de disponibilidad. */
    @Query("""
            select distinct b.room.id from Booking b
            where b.room.location.id = :locationId
              and b.status in :statuses
              and b.startTime < :to
              and b.endTime > :from""")
    List<Long> findBusyRoomIds(@Param("locationId") Long locationId,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to,
                               @Param("statuses") Collection<BookingStatus> statuses);

    /** Reservas activas futuras de un miembro (RN-06). */
    @Query("""
            select count(b) from Booking b
            where b.member.id = :memberId
              and b.status in :statuses
              and b.startTime > :now""")
    long countActiveFutureBookings(@Param("memberId") Long memberId,
                                   @Param("now") LocalDateTime now,
                                   @Param("statuses") Collection<BookingStatus> statuses);

    /**
     * Horas incluidas del plan ya consumidas por el miembro en un mes.
     * Las cancelaciones gratuitas ponen includedHoursApplied en 0 (devolución de horas).
     */
    @Query("""
            select coalesce(sum(b.includedHoursApplied), 0) from Booking b
            where b.member.id = :memberId
              and b.startTime >= :from
              and b.startTime < :to""")
    BigDecimal sumIncludedHoursApplied(@Param("memberId") Long memberId,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    /** Reservas de un miembro en un mes, para el reporte de consumo. */
    @Query("""
            select b from Booking b
            where b.member.id = :memberId
              and b.startTime >= :from
              and b.startTime < :to
            order by b.startTime""")
    List<Booking> findMemberBookingsInRange(@Param("memberId") Long memberId,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    boolean existsByRoomId(Long roomId);

    boolean existsByMemberId(Long memberId);
}
