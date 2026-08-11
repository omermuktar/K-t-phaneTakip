package com.omermuktar.library.book;

import com.omermuktar.library.book.dto.BookCreateRequest;
import com.omermuktar.library.book.dto.BookResponse;
import com.omermuktar.library.book.dto.BookUpdateRequest;
import com.omermuktar.library.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/books")
@Validated
@Tag(name = "Kitaplar", description = "Kitap kataloğu ve kopya yönetimi")
public class BookController {

  private final BookService bookService;

  public BookController(BookService bookService) {
    this.bookService = bookService;
  }

  @PostMapping
  @Operation(summary = "Yeni kitap oluştur")
  public ResponseEntity<BookResponse> create(@Valid @RequestBody BookCreateRequest request) {
    BookResponse response = bookService.create(request);
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
    return ResponseEntity.created(location).body(response);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Kitap ayrıntısını getir")
  public BookResponse getById(
      @PathVariable @Positive(message = "Kitap kimliği pozitif bir sayı olmalıdır.") Long id) {
    return bookService.getById(id);
  }

  @GetMapping
  @Operation(summary = "Kitapları ara ve filtrele")
  public PageResponse<BookResponse> search(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) Boolean available,
      @ParameterObject @PageableDefault(size = 20, sort = "title") Pageable pageable) {
    return PageResponse.from(bookService.search(query, category, available, pageable));
  }

  @GetMapping("/categories")
  @Operation(summary = "Kategorileri listele")
  public List<String> listCategories() {
    return bookService.listCategories();
  }

  @PutMapping("/{id}")
  @Operation(summary = "Kitap bilgilerini güncelle")
  public BookResponse update(
      @PathVariable @Positive(message = "Kitap kimliği pozitif bir sayı olmalıdır.") Long id,
      @Valid @RequestBody BookUpdateRequest request) {
    return bookService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Kitabı sil")
  public ResponseEntity<Void> delete(
      @PathVariable @Positive(message = "Kitap kimliği pozitif bir sayı olmalıdır.") Long id) {
    bookService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
