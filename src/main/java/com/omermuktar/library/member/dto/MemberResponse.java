package com.omermuktar.library.member.dto;

import com.omermuktar.library.member.Member;
import java.time.Instant;

public record MemberResponse(
    Long id,
    String membershipNumber,
    String fullName,
    String email,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {
  public static MemberResponse from(Member member) {
    return new MemberResponse(
        member.getId(),
        member.getMembershipNumber(),
        member.getFullName(),
        member.getEmail(),
        member.isActive(),
        member.getCreatedAt(),
        member.getUpdatedAt());
  }
}
