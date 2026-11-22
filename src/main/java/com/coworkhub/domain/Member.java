package com.coworkhub.domain;

import com.coworkhub.domain.enums.MemberStatus;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/** Miembro del coworking. Documento y correo son únicos (RN-11). */
@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_number", nullable = false, unique = true, length = 30)
    private String documentNumber;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status = MemberStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private MembershipPlan plan;

    /** Relación 1:1 opcional. La FK está en user_accounts (lado propietario: UserAccount). */
    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY)
    private UserAccount userAccount;

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<Booking> bookings = new ArrayList<>();

    public Member() {
    }

    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public MemberStatus getStatus() { return status; }
    public void setStatus(MemberStatus status) { this.status = status; }
    public MembershipPlan getPlan() { return plan; }
    public void setPlan(MembershipPlan plan) { this.plan = plan; }
    public UserAccount getUserAccount() { return userAccount; }
    public void setUserAccount(UserAccount userAccount) { this.userAccount = userAccount; }
    public List<Booking> getBookings() { return bookings; }
}
