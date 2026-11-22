package com.coworkhub.domain;

import com.coworkhub.domain.enums.Role;
import jakarta.persistence.*;

/**
 * Cuenta de acceso al sistema. Se llama UserAccount (no User) para no chocar con
 * org.springframework.security.core.userdetails.User.
 * <p>
 * ADMIN y RECEPTIONIST son empleados: no tienen miembro asociado. Las cuentas MEMBER
 * están vinculadas a su Member (relación 1:1 opcional).
 */
@Entity
@Table(name = "user_accounts")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** Hash BCrypt. Nunca se expone en los DTO. */
    @Column(nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "member_id", unique = true)
    private Member member;

    public UserAccount() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
}
