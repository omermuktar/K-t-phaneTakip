package com.omermuktar.library.member;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

  boolean existsByMembershipNumberIgnoreCase(String membershipNumber);

  boolean existsByMembershipNumberIgnoreCaseAndIdNot(String membershipNumber, Long id);

  boolean existsByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

  Optional<Member> findByMembershipNumberIgnoreCase(String membershipNumber);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select m from Member m where m.id = :id")
  Optional<Member> findByIdForUpdate(@Param("id") Long id);

  @Query(
      """
      select m from Member m
      where (:query is null
             or lower(m.fullName) like lower(concat('%', :query, '%'))
             or lower(m.membershipNumber) like lower(concat('%', :query, '%'))
             or lower(m.email) like lower(concat('%', :query, '%')))
        and (:active is null or m.active = :active)
      """)
  Page<Member> search(
      @Param("query") String query, @Param("active") Boolean active, Pageable pageable);

  long countByActiveTrue();
}
