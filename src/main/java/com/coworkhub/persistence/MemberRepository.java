package com.coworkhub.persistence;

import com.coworkhub.domain.Member;
import com.coworkhub.domain.enums.MemberStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    /** Búsqueda con filtros opcionales por nombre (contiene) y estado. Trae el plan en la misma consulta. */
    @EntityGraph(attributePaths = {"plan", "userAccount"})
    @Query("""
            select m from Member m
            where (:name is null or lower(m.fullName) like lower(concat('%', :name, '%')))
              and (:status is null or m.status = :status)
            order by m.id""")
    List<Member> search(@Param("name") String name, @Param("status") MemberStatus status);

    @EntityGraph(attributePaths = {"plan", "userAccount"})
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findWithPlanById(@Param("id") Long id);

    /**
     * Bloqueo pesimista sobre el miembro: serializa las reservas simultáneas de un mismo miembro
     * para que el límite del plan (RN-06) y el cálculo de horas incluidas (RN-07) no se rompan.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m join fetch m.plan where m.id = :id")
    Optional<Member> findByIdForUpdate(@Param("id") Long id);

    boolean existsByDocumentNumber(String documentNumber);

    boolean existsByDocumentNumberAndIdNot(String documentNumber, Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByPlanId(Long planId);
}
