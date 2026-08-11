package com.omermuktar.library.book.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BookCreateRequest(
    @NotBlank(message = "ISBN boş bırakılamaz.")
        @Pattern(regexp = "^[0-9Xx-]{10,17}$", message = "Geçerli bir ISBN girilmelidir.")
        String isbn,
    @NotBlank(message = "Kitap adı boş bırakılamaz.")
        @Size(max = 180, message = "Kitap adı en fazla 180 karakter olabilir.")
        String title,
    @NotBlank(message = "Yazar adı boş bırakılamaz.")
        @Size(max = 120, message = "Yazar adı en fazla 120 karakter olabilir.")
        String author,
    @NotBlank(message = "Kategori boş bırakılamaz.")
        @Size(max = 80, message = "Kategori en fazla 80 karakter olabilir.")
        String category,
    @Min(value = 1, message = "Toplam kopya sayısı en az 1 olmalıdır.")
        @Max(value = 10_000, message = "Toplam kopya sayısı en fazla 10000 olabilir.")
        int totalCopies) {}
