package com.coworkhub.business.impl;

import com.coworkhub.business.MemberService;
import com.coworkhub.domain.Booking;
import com.coworkhub.domain.Member;
import com.coworkhub.domain.MembershipPlan;
import com.coworkhub.domain.UserAccount;
import com.coworkhub.domain.enums.BookingStatus;
import com.coworkhub.domain.enums.MemberStatus;
import com.coworkhub.domain.enums.Role;
import com.coworkhub.dto.member.ConsumptionResponse;
import com.coworkhub.dto.member.MemberRequest;
import com.coworkhub.dto.member.MemberResponse;
import com.coworkhub.exception.DuplicateResourceException;
import com.coworkhub.exception.ResourceInUseException;
import com.coworkhub.exception.ResourceNotFoundException;
import com.coworkhub.mapper.MemberMapper;
import com.coworkhub.persistence.BookingRepository;
import com.coworkhub.persistence.MemberRepository;
import com.coworkhub.persistence.MembershipPlanRepository;
import com.coworkhub.persistence.UserAccountRepository;
import com.coworkhub.security.CurrentUserProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.function.Function;

@Service
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final MembershipPlanRepository planRepository;
    private final UserAccountRepository accountRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUser;
    private final MemberMapper mapper;

    public MemberServiceImpl(MemberRepository memberRepository, MembershipPlanRepository planRepository,
                             UserAccountRepository accountRepository, BookingRepository bookingRepository,
                             PasswordEncoder passwordEncoder, CurrentUserProvider currentUser, MemberMapper mapper) {
        this.memberRepository = memberRepository;
        this.planRepository = planRepository;
        this.accountRepository = accountRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Override
    public List<MemberResponse> findAll(String name, MemberStatus status) {
        String filter = name == null || name.isBlank() ? null : name.trim();
        return memberRepository.search(filter, status).stream().map(mapper::toResponse).toList();
    }

    @Override
    public MemberResponse findById(Long id) {
        currentUser.checkCanAccessMember(id);
        return mapper.toResponse(get(id));
    }

    @Override
    @Transactional
    public MemberResponse create(MemberRequest request) {
        String document = request.documento().trim();
        String email = request.email().trim().toLowerCase();
        if (memberRepository.existsByDocumentNumber(document)) {
            throw new DuplicateResourceException("Ya existe un miembro con el documento " + document);
        }
        if (memberRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Ya existe un miembro con el correo " + email);
        }
        boolean withAccount = request.usuario() != null && !request.usuario().isBlank();
        if (withAccount && accountRepository.existsByUsernameIgnoreCase(request.usuario().trim())) {
            throw new DuplicateResourceException("El nombre de usuario '" + request.usuario().trim() + "' ya existe");
        }

        Member member = new Member();
        member.setDocumentNumber(document);
        member.setFullName(request.nombreCompleto().trim());
        member.setEmail(email);
        member.setStatus(MemberStatus.ACTIVE);
        member.setPlan(getPlan(request.planId()));
        memberRepository.save(member);

        if (withAccount) {
            UserAccount account = new UserAccount();
            account.setUsername(request.usuario().trim());
            account.setPassword(passwordEncoder.encode(request.password()));
            account.setRole(Role.MEMBER);
            account.setMember(member);
            accountRepository.save(account);
            member.setUserAccount(account);
        }
        return mapper.toResponse(member);
    }

    /** Actualiza datos personales y plan. Los datos de cuenta se gestionan solo en el registro. */
    @Override
    @Transactional
    public MemberResponse update(Long id, MemberRequest request) {
        Member member = get(id);
        String document = request.documento().trim();
        String email = request.email().trim().toLowerCase();
        if (memberRepository.existsByDocumentNumberAndIdNot(document, id)) {
            throw new DuplicateResourceException("Ya existe un miembro con el documento " + document);
        }
        if (memberRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new DuplicateResourceException("Ya existe un miembro con el correo " + email);
        }
        member.setDocumentNumber(document);
        member.setFullName(request.nombreCompleto().trim());
        member.setEmail(email);
        member.setPlan(getPlan(request.planId()));
        return mapper.toResponse(member);
    }

    @Override
    @Transactional
    public MemberResponse suspend(Long id) {
        Member member = get(id);
        member.setStatus(MemberStatus.SUSPENDED);
        return mapper.toResponse(member);
    }

    @Override
    @Transactional
    public MemberResponse activate(Long id) {
        Member member = get(id);
        member.setStatus(MemberStatus.ACTIVE);
        return mapper.toResponse(member);
    }

    /**
     * RN-12: no se elimina un miembro con reservas. Su cuenta de usuario (si tiene) forma parte
     * del registro del miembro y se elimina con él.
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Member member = get(id);
        if (bookingRepository.existsByMemberId(id)) {
            throw new ResourceInUseException("No se puede eliminar el miembro porque tiene reservas registradas");
        }
        if (member.getUserAccount() != null) {
            accountRepository.delete(member.getUserAccount());
        }
        memberRepository.delete(member);
    }

    @Override
    public ConsumptionResponse getMonthlyConsumption(Long id, YearMonth month) {
        currentUser.checkCanAccessMember(id);
        Member member = get(id);
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();

        List<Booking> bookings = bookingRepository.findMemberBookingsInRange(id, from, to);
        List<Booking> billable = bookings.stream().filter(b -> b.getStatus() != BookingStatus.CANCELLED).toList();
        long cancelled = bookings.size() - billable.size();

        BigDecimal includedUsed = sum(bookings, Booking::getIncludedHoursApplied);
        BigDecimal hoursBooked = sum(billable, Booking::durationHours);
        BigDecimal overage = sum(billable, b -> b.durationHours().subtract(b.getIncludedHoursApplied()));
        BigDecimal roomCost = sum(billable, Booking::getRoomCost);
        BigDecimal servicesCost = sum(billable, Booking::getServicesCost);
        BigDecimal penalties = sum(bookings, Booking::getCancellationFee);

        BigDecimal planHours = BigDecimal.valueOf(member.getPlan().getIncludedHoursPerMonth());
        BigDecimal available = planHours.subtract(includedUsed).max(BigDecimal.ZERO);

        return new ConsumptionResponse(member.getId(), member.getFullName(), month.toString(),
                member.getPlan().getName(), member.getPlan().getIncludedHoursPerMonth(),
                includedUsed, available, hoursBooked, overage, billable.size(), cancelled,
                roomCost, servicesCost, penalties, roomCost.add(servicesCost).add(penalties));
    }

    private static BigDecimal sum(List<Booking> bookings, Function<Booking, BigDecimal> field) {
        return bookings.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private Member get(Long id) {
        return memberRepository.findWithPlanById(id).orElseThrow(() -> new ResourceNotFoundException("Miembro", id));
    }

    private MembershipPlan getPlan(Long id) {
        return planRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Plan", id));
    }
}
