package com.omermuktar.library.member;

import com.omermuktar.library.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
    name = "members",
    uniqueConstraints = {
      @UniqueConstraint(name = "uq_members_number", columnNames = "membership_number"),
      @UniqueConstraint(name = "uq_members_email", columnNames = "email")
    })
public class Member extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "membership_number", nullable = false, length = 30)
  private String membershipNumber;

  @Column(name = "full_name", nullable = false, length = 120)
  private String fullName;

  @Column(nullable = false, length = 180)
  private String email;

  @Column(nullable = false)
  private boolean active;

  @Version
  @Column(nullable = false)
  private long version;

  protected Member() {}

  private Member(String membershipNumber, String fullName, String email) {
    this.membershipNumber = normalizeMembershipNumber(membershipNumber);
    this.fullName = fullName.trim();
    this.email = email.trim().toLowerCase();
    this.active = true;
  }

  public static Member create(String membershipNumber, String fullName, String email) {
    return new Member(membershipNumber, fullName, email);
  }

  public void updateDetails(
      String membershipNumber, String fullName, String email, boolean active) {
    this.membershipNumber = normalizeMembershipNumber(membershipNumber);
    this.fullName = fullName.trim();
    this.email = email.trim().toLowerCase();
    this.active = active;
  }

  private static String normalizeMembershipNumber(String membershipNumber) {
    return membershipNumber.trim().toUpperCase();
  }

  public Long getId() {
    return id;
  }

  public String getMembershipNumber() {
    return membershipNumber;
  }

  public String getFullName() {
    return fullName;
  }

  public String getEmail() {
    return email;
  }

  public boolean isActive() {
    return active;
  }

  public long getVersion() {
    return version;
  }
}
