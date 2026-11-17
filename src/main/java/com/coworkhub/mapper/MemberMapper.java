package com.coworkhub.mapper;

import com.coworkhub.domain.Member;
import com.coworkhub.domain.UserAccount;
import com.coworkhub.dto.member.MemberResponse;
import org.springframework.stereotype.Component;

@Component
public class MemberMapper {

    public MemberResponse toResponse(Member m) {
        UserAccount account = m.getUserAccount();
        return new MemberResponse(m.getId(), m.getDocumentNumber(), m.getFullName(), m.getEmail(), m.getStatus(),
                m.getPlan().getId(), m.getPlan().getName(), account == null ? null : account.getUsername());
    }
}
