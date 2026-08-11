package com.omermuktar.library.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberCreateRequest(
    @NotBlank(message = "Üye numarası boş bırakılamaz.")
        @Pattern(regexp = "^[A-Za-z0-9-]{3,30}$", message = "Üye numarası 3-30 karakter olmalıdır.")
        String membershipNumber,
    @NotBlank(message = "Ad soyad boş bırakılamaz.")
        @Size(max = 120, message = "Ad soyad en fazla 120 karakter olabilir.")
        String fullName,
    @NotBlank(message = "E-posta adresi boş bırakılamaz.")
        @Email(message = "Geçerli bir e-posta adresi girilmelidir.")
        @Size(max = 180, message = "E-posta adresi en fazla 180 karakter olabilir.")
        String email) {}
