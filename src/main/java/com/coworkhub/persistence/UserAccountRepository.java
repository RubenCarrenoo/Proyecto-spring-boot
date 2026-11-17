package com.coworkhub.persistence;

import com.coworkhub.domain.UserAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    @EntityGraph(attributePaths = {"member"})
    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByMemberId(Long memberId);
}
