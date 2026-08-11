package com.omermuktar.library.member;

import com.omermuktar.library.common.exception.ConflictException;
import com.omermuktar.library.common.exception.ResourceNotFoundException;
import com.omermuktar.library.loan.LoanRepository;
import com.omermuktar.library.loan.LoanStatus;
import com.omermuktar.library.member.dto.MemberCreateRequest;
import com.omermuktar.library.member.dto.MemberResponse;
import com.omermuktar.library.member.dto.MemberUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberService {

  private final MemberRepository memberRepository;
  private final LoanRepository loanRepository;

  public MemberService(MemberRepository memberRepository, LoanRepository loanRepository) {
    this.memberRepository = memberRepository;
    this.loanRepository = loanRepository;
  }

  @Transactional
  public MemberResponse create(MemberCreateRequest request) {
    validateUniqueness(null, request.membershipNumber(), request.email());
    Member member = Member.create(request.membershipNumber(), request.fullName(), request.email());
    return MemberResponse.from(memberRepository.saveAndFlush(member));
  }

  public MemberResponse getById(Long id) {
    return MemberResponse.from(findMember(id));
  }

  public Page<MemberResponse> search(String query, Boolean active, Pageable pageable) {
    String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
    return memberRepository.search(normalizedQuery, active, pageable).map(MemberResponse::from);
  }

  @Transactional
  public MemberResponse update(Long id, MemberUpdateRequest request) {
    Member member = findMemberForUpdate(id);
    if (!request.active() && loanRepository.existsByMemberIdAndStatus(id, LoanStatus.ACTIVE)) {
      throw new ConflictException(
          "MEMBER_HAS_ACTIVE_LOANS", "Aktif ödünç kaydı bulunan üye pasif duruma alınamaz.");
    }
    validateUniqueness(id, request.membershipNumber(), request.email());
    member.updateDetails(
        request.membershipNumber(), request.fullName(), request.email(), request.active());
    return MemberResponse.from(memberRepository.saveAndFlush(member));
  }

  @Transactional
  public void delete(Long id) {
    Member member = findMemberForUpdate(id);
    if (loanRepository.existsByMemberId(id)) {
      throw new ConflictException(
          "MEMBER_HAS_LOAN_HISTORY", "Ödünç geçmişi bulunan üye silinemez.");
    }
    memberRepository.delete(member);
  }

  private void validateUniqueness(Long currentId, String membershipNumber, String email) {
    boolean numberExists =
        currentId == null
            ? memberRepository.existsByMembershipNumberIgnoreCase(membershipNumber)
            : memberRepository.existsByMembershipNumberIgnoreCaseAndIdNot(
                membershipNumber, currentId);
    if (numberExists) {
      throw new ConflictException(
          "MEMBERSHIP_NUMBER_EXISTS", "Bu üye numarası zaten kullanılıyor.");
    }

    boolean emailExists =
        currentId == null
            ? memberRepository.existsByEmailIgnoreCase(email)
            : memberRepository.existsByEmailIgnoreCaseAndIdNot(email, currentId);
    if (emailExists) {
      throw new ConflictException("MEMBER_EMAIL_EXISTS", "Bu e-posta adresi zaten kullanılıyor.");
    }
  }

  private Member findMember(Long id) {
    return memberRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Üye", id));
  }

  private Member findMemberForUpdate(Long id) {
    return memberRepository
        .findByIdForUpdate(id)
        .orElseThrow(() -> new ResourceNotFoundException("Üye", id));
  }
}
